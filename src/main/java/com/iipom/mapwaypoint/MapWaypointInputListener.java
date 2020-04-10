package com.iipom.mapwaypoint;

import net.runelite.client.input.MouseListener;
import javax.inject.Inject;
import java.awt.event.MouseEvent;

public class MapWaypointInputListener implements MouseListener {

    private final MapWaypointPlugin plugin;

    @Inject
    private MapWaypointInputListener(MapWaypointPlugin plugin) { this.plugin = plugin; }

    @Override
    public MouseEvent mouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == 1 && (mouseEvent.getClickCount() == 2 || (plugin.getConfig().shiftClick() && mouseEvent.isShiftDown()))) {
            plugin.mouseClicked(mouseEvent);
        }

        return mouseEvent;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent mouseEvent) { return mouseEvent; }

    @Override
    public MouseEvent mouseReleased(MouseEvent mouseEvent) { return mouseEvent; }

    @Override
    public MouseEvent mouseEntered(MouseEvent mouseEvent) { return mouseEvent; }

    @Override
    public MouseEvent mouseExited(MouseEvent mouseEvent) { return mouseEvent; }

    @Override
    public MouseEvent mouseDragged(MouseEvent mouseEvent) { return mouseEvent; }

    @Override
    public MouseEvent mouseMoved(MouseEvent mouseEvent) { return mouseEvent; }
}
