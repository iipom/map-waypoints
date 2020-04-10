package com.iipom.mapwaypoint;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("mapwaypoint")
public interface MapWaypointConfig extends Config {

    @ConfigItem(
            keyName = "shiftClick",
            name = "Shift-click waypoins",
            description = "Set and remove waypoints with shift-click"
    )
    default boolean shiftClick() { return true; }
}
