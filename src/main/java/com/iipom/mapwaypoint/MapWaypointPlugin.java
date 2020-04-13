package com.iipom.mapwaypoint;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.RenderOverview;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
        name = "Map Waypoints",
        description = "Adds waypoint functionality to the world map (via double-click) with a direction overlay",
        tags = {"map", "waypoint", "distance"}
)
public class MapWaypointPlugin extends Plugin
{

    private static final BufferedImage WAYPOINT_ICON;

    static
    {
        WAYPOINT_ICON = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        BufferedImage waypointIcon = ImageUtil.getResourceStreamFromClass(MapWaypointPlugin.class, "waypoint.png");
        WAYPOINT_ICON.getGraphics().drawImage(waypointIcon, 8, 8, null);
    }

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
    private WaypointTileOverlay waypointTileOverlay;

    public void mouseClicked(MouseEvent mouseEvent)
    {
        if (client.getRenderOverview().getWorldMapManager().isLoaded())
        {
            if (waypoint != null && waypoint.getClickbox().contains(mouseEvent.getPoint()))
            {
                worldMapPointManager.remove(waypoint);

                waypoint = null;
            }
            else
            {
                float zoom = client.getRenderOverview().getWorldMapZoom();

                if (zoom == 0.0)
                {
                    return;
                }

                WorldPoint destination = calculateMapPoint(client.getRenderOverview(), zoom);

                worldMapPointManager.removeIf(x -> x == waypoint);
                waypoint = new WorldMapPoint(destination, WAYPOINT_ICON);
                worldMapPointManager.add(waypoint);
            }
        }
    }

    private WorldPoint calculateMapPoint(RenderOverview renderOverview, float zoom)
    {
        WorldPoint mapPoint = new WorldPoint(renderOverview.getWorldMapPosition().getX(), renderOverview.getWorldMapPosition().getY(), 0);

        Point middle = worldMapOverlay.mapWorldPointToGraphicsPoint(mapPoint);
        Point mouse = client.getMouseCanvasPosition();

        int dx = (int) ((mouse.getX() - middle.getX()) / zoom);
        int dy = (int) ((-(mouse.getY() - middle.getY())) / zoom);

        return mapPoint.dx(dx).dy(dy);
    }

    @Override
    public void startUp()
    {
        mouseManager.registerMouseListener(inputListener);

        overlayManager.add(waypointArrowOverlay);
        overlayManager.add(waypointTileOverlay);

        waypoint = null;
    }

    @Override
    public void shutDown()
    {
        mouseManager.unregisterMouseListener(inputListener);

        overlayManager.remove(waypointArrowOverlay);
        overlayManager.remove(waypointTileOverlay);

        worldMapPointManager.removeIf(x -> x == waypoint);

        waypoint = null;
    }

    @Provides
    MapWaypointConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MapWaypointConfig.class);
    }
}
