package com.iipom.mapwaypoint;

import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

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
        WAYPOINT_ICON.getGraphics().drawImage(waypointIcon, 0, 0, null);
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
                try
                {
                    List<WorldMapPoint> worldMapPoints = getWorldMapPoints();

                    addWaypoint(worldMapPoints, mouseEvent.getX(), mouseEvent.getY());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addWaypoint(List<WorldMapPoint> worldMapPoints, int mouseX, int mouseY)
    {
        worldMapPointManager.removeIf(x -> x == waypoint);

        double scale = calculateScale(worldMapPoints);

        if (scale == 0.0)
        {
            return;
        }

        WorldMapPoint worldMapPoint = worldMapPoints.stream()
                .filter(x -> (x.getClickbox() != null && x.getClickbox().getX() > 0 && x.getClickbox().getY() > 0))
                .findAny()
                .get();

        WorldPoint destination = calculateMapPoint(worldMapPoint, mouseX, mouseY, scale);

        waypoint = new WorldMapPoint(destination, WAYPOINT_ICON);

        worldMapPointManager.add(waypoint);
    }

    private double calculateScale(List<WorldMapPoint> worldMapPoints)
    {
        WorldMapPoint p1 = null;
        WorldMapPoint p2 = null;

        for (WorldMapPoint worldMapPoint : worldMapPoints)
        {
            if (worldMapPoint.getClickbox() != null)
            {
                if (p1 == null)
                {
                    p1 = worldMapPoint;
                }
                else
                {
                    p2 = worldMapPoint;
                    break;
                }
            }
        }

        if (p1 == null || p2 == null)
        {
            return 0.0;
        }

        return (p1.getClickbox().getX() - p2.getClickbox().getX()) / (p1.getWorldPoint().getX() - p2.getWorldPoint().getX());
    }

    private WorldPoint calculateMapPoint(WorldMapPoint worldMapPoint, int mouseX, int mouseY, double scale)
    {
        int mapPointScreenX = (int) worldMapPoint.getClickbox().getX();
        int mapPointScreenY = (int) worldMapPoint.getClickbox().getY();

        int screenDx = mouseX - mapPointScreenX;
        int screenDy = -(mouseY - mapPointScreenY);

        int dx = (int) (screenDx / scale);
        int dy = (int) (screenDy / scale);

        return worldMapPoint.getWorldPoint().dx(dx).dy(dy);
    }

    // This method sucks
    private List<WorldMapPoint> getWorldMapPoints() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Method getWorldMapPoints = worldMapPointManager.getClass().getDeclaredMethod("getWorldMapPoints");

        getWorldMapPoints.setAccessible(true);

        return (List<WorldMapPoint>) getWorldMapPoints.invoke(worldMapPointManager);
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
