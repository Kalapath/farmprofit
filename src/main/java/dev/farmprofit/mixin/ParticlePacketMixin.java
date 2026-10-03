package dev.farmprofit.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sees particles the server sends (needed for the treasure chest lockpick helper).
 * require = 0: if this Minecraft version names the method differently, the hook is skipped instead of crashing.
 */
@Mixin(ClientPacketListener.class)
public abstract class ParticlePacketMixin {
    @Inject(method = "handleParticleEvent", at = @At("TAIL"), require = 0)
    private void skyassist$onParticles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        dev.farmprofit.Lockpick.onPacket(packet);
    }
}
