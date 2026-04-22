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
 * Fişek roketleri spawn edildiğinde bunu algılar.
 * Eğer trap aktif ve hedef bu fişeği kullanıyorsa → knockback spam başlatır.
 */
@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin {

    /**
     * Fişek oluşturulduğunda çalışır.
     * Eğer bu fişeği atan hedef oyuncu ise TrapHandler'a haber verir.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onFireworkTick(CallbackInfo ci) {
        FireworkRocketEntity self = (FireworkRocketEntity)(Object)this;
        MinecraftClient mc = MinecraftClient.getInstance();

        if (!TrapHandler.trapActive) return;
        if (mc.world == null || mc.player == null) return;

        PlayerEntity target = TrapHandler.getTrapTarget();
        if (target == null) return;

        // Bu fişeğin sahibi (shooter) hedef mi?
        if (self.getOwner() instanceof PlayerEntity shooter) {
            if (shooter.getUuid().equals(target.getUuid())) {
                // Fişek henüz patlamamışsa sayaç düşürülmesin diye sadece 1 kez say
                // "shotDetected" durumu için basit bir flag
                if (!elytrap_counted) {
                    elytrap_counted = true;
                    // 1 fişek = fireworkKnockbackTicks tick knockback ekle
                    TrapHandler.onFireworkDetected(1);
                }
            }
        }
    }

    // Her fişek instance başına bir kez sayılması için flag
    private boolean elytrap_counted = false;
}

