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
 * Fişeği atan oyuncuyu (shooter) TrapHandler'a her tick gönderir.
 * TrapHandler mesafe kontrolünü ve wasInFireworkMode takibini kendisi yapar.
 *
 * NOT: elytrap_counted kaldırıldı — her tick TrapHandler'a bildirim gider,
 * TrapHandler içinde "zaten modundaysa sadece sayacı sıfırla" mantığı var.
 * Bu sayede fişek kesme ve yeniden başlatma doğru tespit edilir.
 */
@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onFireworkTick(CallbackInfo ci) {
        if (!TrapHandler.active) return;

        FireworkRocketEntity self = (FireworkRocketEntity)(Object)this;
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null || mc.player == null) return;

        // Sadece başka bir oyuncu attıysa işle
        if (self.getOwner() instanceof PlayerEntity shooter) {
            if (!shooter.getUuid().equals(mc.player.getUuid())) {
                // Her tick bildir — TrapHandler içinde tekrar tetikleme korunuyor
                TrapHandler.onFireworkDetected(shooter);
            }
        }
    }
}

