package com.elytrap.mixin;

import com.elytrap.handler.TrapHandler;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FireworkRocketEntityMixin
 *
 * Herhangi bir oyuncu fişek kullandığında bir kez tetiklenir.
 * TrapHandler'a haber verir → slot switch yapar.
 */
@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin {

    // Her fişek instance'ı sadece 1 kez sayılsın
    private boolean elytrap_counted = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onFireworkTick(CallbackInfo ci) {
        if (elytrap_counted) return;
        if (!TrapHandler.active) return;

        FireworkRocketEntity self = (FireworkRocketEntity)(Object)this;
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null || mc.player == null) return;

        // Fişeği atan oyuncu biz değilsek (önümüzdeki adam)
        if (self.getOwner() instanceof PlayerEntity shooter) {
            if (!shooter.getUuid().equals(mc.player.getUuid())) {
                elytrap_counted = true;
                TrapHandler.onFireworkDetected();
            }
        }
    }
}
