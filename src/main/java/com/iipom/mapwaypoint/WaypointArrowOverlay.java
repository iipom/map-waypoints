package com.iipom.mapwaypoint;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.*;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

public class WaypointArrowOverlay extends Overlay
{

    private static final BufferedImage ARROW_ICON = ImageUtil.getResourceStreamFromClass(MapWaypointPlugin.class, "arrow.png");

    private final Client client;
    private final MapWaypointPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();
    private final TitleComponent stepsComponent = TitleComponent.builder().build();

    @Inject
    private WaypointArrowOverlay(Client client, MapWaypointPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_CENTER);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (plugin.getWaypoint() == null)
        {
            return null;
        }

        Player player = client.getLocalPlayer();
        if (player == null)
        {
            return null;
        }

        WorldPoint currentLocation = player.getWorldLocation();
        WorldPoint destination = plugin.getWaypoint().getWorldPoint();

        int distance = currentLocation.distanceTo(destination);
        String steps = "Steps: " + distance;

        BufferedImage arrow = calculateImageRotation(currentLocation, destination, graphics.getFontMetrics().stringWidth(steps));

        stepsComponent.setText(steps);
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(new ImageComponent(arrow));
        panelComponent.getChildren().add(stepsComponent);
        panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth(steps) + 10, 0));

        return panelComponent.render(graphics);
    }

    private BufferedImage calculateImageRotation(WorldPoint currentLocation, WorldPoint destination, int textLen)
    {
        double angle = calculateAngle(currentLocation, destination);
        int dx = (textLen - ARROW_ICON.getWidth()) / 2;

        BufferedImage rotatedImage = ImageUtil.rotateImage(ARROW_ICON, 2.0 * Math.PI - angle);
        BufferedImage finalImage = new BufferedImage(rotatedImage.getWidth() + dx, 27, BufferedImage.TYPE_INT_ARGB);
        finalImage.getGraphics().drawImage(rotatedImage, dx, 0, null);

        return finalImage;
    }

    private double calculateAngle(WorldPoint currentLocation, WorldPoint destination)
    {
        int dx = destination.getX() - currentLocation.getX();
        int dy = destination.getY() - currentLocation.getY();

        double angle = Math.atan2(dy, dx);
        double clientAngle = (client.getMapAngle() / 2048.0) * 2.0 * Math.PI;

        return angle - clientAngle;
    }
}
