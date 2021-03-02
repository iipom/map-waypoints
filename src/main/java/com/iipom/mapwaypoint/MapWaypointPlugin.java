package com.iipom.mapwaypoint;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

@Slf4j
@PluginDescriptor(
        name = "Map Waypoints",
        description = "Adds waypoint functionality to the world map (via double-click) with a direction overlay",
        tags = {"map", "waypoint", "distance"}
)
public class MapWaypointPlugin extends Plugin
{
    private static final String WALK_HERE = "Walk here";
    private static final String CLOSE = "Close";
    private static final String CANCEL = "Cancel";
    private static final String SET = "Set";
    private static final String FOCUS = "Focus";
    private static final String REMOVE = "Remove";
    private static final String WAYPOINT = "<col=ffff>Waypoint</col>";

    private static final BufferedImage WAYPOINT_ICON;

    static
    {
        WAYPOINT_ICON = new BufferedImage(37, 37, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage waypointIcon = ImageUtil.loadImageResource(MapWaypointPlugin.class, "waypoint.png");
        WAYPOINT_ICON.getGraphics().drawImage(waypointIcon, 0, 0, null);
    }

    private Point lastMenuOpenedPoint;

    @Getter(AccessLevel.PACKAGE)
    private WorldMapPoint waypoint;

    @Inject
    private Client client;

    @Inject
    private MapWaypointConfig config;

    @Inject
    private MapWaypointInputListener inputListener;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private WorldMapPointManager worldMapPointManager;

    @Inject
    private WorldMapOverlay worldMapOverlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private WaypointArrowOverlay waypointArrowOverlay;

    @Inject
    private WaypointMinimapOverlay waypointMinimapOverlay;

    @Inject
    private WaypointTileOverlay waypointTileOverlay;

    public void mouseClicked()
    {
        if (isMouseInWorldMap())
        {
            final Point mousePos = client.getMouseCanvasPosition();

            if (waypoint != null && waypoint.getClickbox().contains(mousePos.getX(), mousePos.getY()))
            {
                removeWaypoint();
            }
            else
            {
                setWaypoint(mousePos);
            }
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        if (isMouseInWorldMap())
        {
            lastMenuOpenedPoint = client.getMouseCanvasPosition();
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (isMouseInWorldMap() && event.getOption().equals(CANCEL))
        {
            try
            {
                final Point mousePos = client.getMouseCanvasPosition();

                final boolean insideClickbox = waypoint != null && waypoint.getClickbox().contains(mousePos.getX(), mousePos.getY());

                MenuEntry[] menuEntries = client.getMenuEntries();
                menuEntries = Arrays.copyOf(menuEntries, menuEntries.length + (insideClickbox  ? 2 : 1));
                final MenuEntry point = menuEntries[menuEntries.length - (insideClickbox ? 2 : 1)] = new MenuEntry();

                point.setOption(insideClickbox ? REMOVE : SET);
                point.setTarget(WAYPOINT);
                point.setType(MenuAction.RUNELITE.getId());

                if (insideClickbox)
                {
                    MenuEntry focus = menuEntries[menuEntries.length - 1] = new MenuEntry();
                    focus.setOption(FOCUS);
                    focus.setTarget(WAYPOINT);
                    focus.setType(MenuAction.RUNELITE.getId());
                }

                client.setMenuEntries(menuEntries);
            }
            catch (NullPointerException e)
            {
                // I'll figure out how to fix this someday
            }
        }
        else if (config.drawTile() && waypoint != null && event.getOption().equals(WALK_HERE))
        {
            final Tile selectedSceneTile = client.getSelectedSceneTile();
            if (selectedSceneTile == null)
            {
                return;
            }

            if (selectedSceneTile.getWorldLocation().equals(waypoint.getWorldPoint()))
            {
                MenuEntry[] menuEntries = client.getMenuEntries();
                menuEntries = Arrays.copyOf(menuEntries, menuEntries.length + 1);
                final MenuEntry point = menuEntries[menuEntries.length - 1] = new MenuEntry();

                point.setOption(REMOVE);
                point.setTarget(WAYPOINT);
                point.setType(MenuAction.RUNELITE.getId());

                client.setMenuEntries(menuEntries);
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (event.getMenuAction().getId() != MenuAction.RUNELITE.getId() || !event.getMenuTarget().equals(WAYPOINT) || !client.isMenuOpen())
        {
            return;
        }

        switch (event.getMenuOption())
        {
            case SET:
                setWaypoint(lastMenuOpenedPoint);
                break;
            case REMOVE:
                removeWaypoint();
                break;
            case FOCUS:
                focusWaypoint();
                break;
        }
    }

    @Override
    public void startUp()
    {
        mouseManager.registerMouseListener(inputListener);

        overlayManager.add(waypointArrowOverlay);
        overlayManager.add(waypointMinimapOverlay);
        overlayManager.add(waypointTileOverlay);

        waypoint = null;
    }

    @Override
    public void shutDown()
    {
        mouseManager.unregisterMouseListener(inputListener);

        overlayManager.remove(waypointArrowOverlay);
        overlayManager.remove(waypointMinimapOverlay);
        overlayManager.remove(waypointTileOverlay);

        worldMapPointManager.removeIf(x -> x == waypoint);

        waypoint = null;
    }

    private void setWaypoint(final Point mousePos)
    {
        final RenderOverview renderOverview = client.getRenderOverview();

        final float zoom = renderOverview.getWorldMapZoom();
        final WorldPoint destination = calculateMapPoint(renderOverview, mousePos, zoom);

        worldMapPointManager.removeIf(x -> x == waypoint);
        waypoint = new WorldMapPoint(destination, WAYPOINT_ICON);
        waypoint.setTarget(waypoint.getWorldPoint());
        waypoint.setJumpOnClick(true);
        worldMapPointManager.add(waypoint);

        playSoundEffect();
    }

    private void removeWaypoint()
    {
        if (waypoint != null)
        {
            worldMapPointManager.remove(waypoint);
            waypoint = null;

            playSoundEffect();
        }
    }

    private void focusWaypoint()
    {
        if (waypoint != null)
        {
            client.getRenderOverview().setWorldMapPositionTarget(waypoint.getWorldPoint());
            playSoundEffect();
        }
    }

    private WorldPoint calculateMapPoint(RenderOverview renderOverview, Point mousePos, float zoom)
    {
        final WorldPoint mapPoint = new WorldPoint(renderOverview.getWorldMapPosition().getX(), renderOverview.getWorldMapPosition().getY(), 0);
        final Point middle = worldMapOverlay.mapWorldPointToGraphicsPoint(mapPoint);

        final int dx = (int) ((mousePos.getX() - middle.getX()) / zoom);
        final int dy = (int) ((-(mousePos.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    private boolean isMouseInWorldMap()
    {
        final Point mousePos = client.getMouseCanvasPosition();
        final Widget view = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
        if (view == null)
        {
            return false;
        }

        final Rectangle worldMapBounds = view.getBounds();
        return worldMapBounds.contains(mousePos.getX(), mousePos.getY());
    }

    private void playSoundEffect()
    {
        if (config.playSoundEffect())
        {
            client.playSoundEffect(SoundEffectID.UI_BOOP);
        }
    }

    @Provides
    MapWaypointConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MapWaypointConfig.class);
    }
}
