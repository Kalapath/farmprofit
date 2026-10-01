package dev.farmprofit;

import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;

/** Kept in its own class so nothing loads the Mod API classes unless the mod is installed. */
final class HypixelApiBridge {
    static void register() {
        HypixelModAPI api = HypixelModAPI.getInstance();
        api.subscribeToEventPacket(ClientboundLocationPacket.class);
        api.createHandler(ClientboundLocationPacket.class, packet -> {
            HypixelLocation.serverType = packet.getServerType().map(Object::toString).orElse(null);
            HypixelLocation.mode = packet.getMode().orElse(null);
            HypixelLocation.map = packet.getMap().orElse(null);
        });
    }

    private HypixelApiBridge() {}
}
