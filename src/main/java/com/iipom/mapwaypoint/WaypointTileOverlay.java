package com.iipom.mapwaypoint;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;

public class WaypointTileOverlay extends Overlay {

    private static final int MAX_DRAW_DISTANCE = 32;
    private static final Color TILE_COLOR = new Color(0, 201, 198);

    private final Client client;
    private final MapWaypointPlugin plugin;
    private final MapWaypointConfig config;

    @Inject
    private WaypointTileOverlay(Client client, MapWaypointPlugin plugin, MapWaypointConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (plugin.getWaypoint() == null || !config.drawTile()) return null;

        final WorldPoint waypoint = plugin.getWaypoint().getWorldPoint();

        if (isInSamePlane(client.getPlane(), waypoint.getPlane())) {
            drawTile(graphics, waypoint);
        }

        return null;
    }

    private void drawTile(Graphics2D graphics, WorldPoint waypoint) {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (waypoint.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE) {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, waypoint);
        if (lp == null) {
            return;
        }

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) {
            return;
        }

        OverlayUtil.renderPolygon(graphics, poly, TILE_COLOR);
    }

    private boolean isInSamePlane(int clientPlane, int worldPlane) {
        return clientPlane == worldPlane;
    }
}