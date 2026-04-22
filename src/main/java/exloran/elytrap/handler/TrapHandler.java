package com.elytrap.handler;

import com.elytrap.config.ElyTrapConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * ElyTrap - Trap Handler
 *
 * Ana mantık:
 * 1. TRAP MODU: Hedef oyuncuyu yakala, fişek basınca knockback spam at, traptan çıkmasını engelle.
 * 2. KAÇIŞ MODU: Biz trapa düşersek kendimizi kurtarır.
 */
public class TrapHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ── Trap Modu State ────────────────────────────────────────────────
    public static boolean trapActive = false;
    private static PlayerEntity trapTarget = null;
    private static int attackTimer = 0;
    private static int fireworkKnockbackRemaining = 0; // Fişek algılayınca bu kadar tick sürer

    // ── Kaçış Modu State ──────────────────────────────────────────────
    public static boolean escapeModeActive = false;
    private static int escapeTimer = 0;

    // ── Son bilinen hedef pozisyonu ────────────────────────────────────
    private static Vec3d lastTargetPos = null;

    // ─────────────────────────────────────────────────────────────────
    //  TRAP TOGGLE
    // ─────────────────────────────────────────────────────────────────

    public static void toggleTrap() {
        if (mc.player == null) return;

        trapActive = !trapActive;

        if (trapActive) {
            // En yakın oyuncuyu hedef al
            trapTarget = findNearestPlayer();
            fireworkKnockbackRemaining = 0;
            attackTimer = 0;

            if (trapTarget != null) {
                sendMessage("§c[ElyTrap] §fTrap AKTİF! Hedef: §e" + trapTarget.getName().getString());
            } else {
                sendMessage("§c[ElyTrap] §fTrap AKTİF! §7(Yakında hedef yok)");
            }
        } else {
            trapTarget = null;
            fireworkKnockbackRemaining = 0;
            sendMessage("§a[ElyTrap] §fTrap kapatıldı.");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ESCAPE MODE TOGGLE
    // ─────────────────────────────────────────────────────────────────

    public static void toggleEscapeMode() {
        if (mc.player == null) return;

        escapeModeActive = !escapeModeActive;
        escapeTimer = 0;

        if (escapeModeActive) {
            sendMessage("§e[ElyTrap] §fKaçış modu AKTİF! Elytra ile zıplayarak kaç!");
        } else {
            sendMessage("§7[ElyTrap] §fKaçış modu devre dışı.");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  HER TİK ÇAĞRILIR (ClientTickEvent)
    // ─────────────────────────────────────────────────────────────────

    public static void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (trapActive) {
            handleTrapTick();
        }

        if (escapeModeActive) {
            handleEscapeTick();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  TRAP TİKİ
    // ─────────────────────────────────────────────────────────────────

    private static void handleTrapTick() {
        ClientPlayerEntity self = mc.player;

        // Hedef geçersizse yeniden tara
        if (trapTarget == null || !trapTarget.isAlive() || trapTarget.isRemoved()) {
            trapTarget = findNearestPlayer();
            if (trapTarget == null) return;
        }

        // Hedef elytra ile uçuyor mu kontrol et
        boolean targetIsFlying = trapTarget.isFallFlying(); // Elytra glide

        // ── Fişek Knockback Spam ─────────────────────────────────────
        if (fireworkKnockbackRemaining > 0) {
            attackTimer++;
            if (attackTimer >= ElyTrapConfig.attackCooldownTicks) {
                attackTimer = 0;
                applyKnockbackToTarget();
                fireworkKnockbackRemaining--;
            }
        }

        // ── Traptan çıkmayı engelle ──────────────────────────────────
        // Hedef belirli bir mesafeyi geçmeye çalışıyorsa onu geri it
        if (lastTargetPos != null) {
            double dist = trapTarget.getPos().distanceTo(lastTargetPos);
            if (dist > ElyTrapConfig.trapBoundaryRadius) {
                // Sınırı geçti - knockback ile geri it
                sendKnockbackPacket(trapTarget, lastTargetPos);
                if (ElyTrapConfig.debugMode) {
                    sendMessage("§7[DEBUG] Hedef sınırı geçti! Geri itiliyor.");
                }
            }
        }

        lastTargetPos = trapTarget.getPos();
    }

    // ─────────────────────────────────────────────────────────────────
    //  KAÇIŞ TİKİ
    // ─────────────────────────────────────────────────────────────────

    private static void handleEscapeTick() {
        ClientPlayerEntity self = mc.player;
        escapeTimer++;

        // Her 3 tikte bir kendine yukarı ve rastgele yön ver
        if (escapeTimer % 3 == 0) {
            Vec3d velocity = self.getVelocity();

            // Elytra aktifse yukarı ve öne fırlat
            if (self.isFallFlying()) {
                Vec3d lookDir = self.getRotationVec(1.0f);
                double pushX = lookDir.x * ElyTrapConfig.escapePushStrength;
                double pushY = 0.5; // Yukarı
                double pushZ = lookDir.z * ElyTrapConfig.escapePushStrength;

                self.setVelocity(velocity.x + pushX, velocity.y + pushY, velocity.z + pushZ);
            }
        }

        // 100 tik sonra escape modu otomatik kapansın
        if (escapeTimer >= 100) {
            escapeModeActive = false;
            escapeTimer = 0;
            sendMessage("§7[ElyTrap] §fKaçış modu sona erdi.");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  FİŞEK ALGILANDI - Mixin'den çağrılır
    // ─────────────────────────────────────────────────────────────────

    /**
     * Mixin, hedef oyuncu fişek kullandığında bu metodu çağırır.
     * Ne kadar fişek = o kadar tick knockback spam
     */
    public static void onFireworkDetected(int fireworkCount) {
        if (!trapActive) return;

        // Her fişek için N tik knockback spam ekle
        int newTicks = fireworkCount * ElyTrapConfig.fireworkKnockbackTicks;
        fireworkKnockbackRemaining += newTicks;
        attackTimer = 0; // Anında başlat

        sendMessage("§c[ElyTrap] §f" + fireworkCount + " fişek! " + newTicks + " tick knockback spam başlıyor...");
    }

    // ─────────────────────────────────────────────────────────────────
    //  KNOCKBACK UYGULA
    // ─────────────────────────────────────────────────────────────────

    private static void applyKnockbackToTarget() {
        if (trapTarget == null || mc.player == null) return;

        // Hedefin fişeği bastığı yönü hesapla (bakış yönü)
        Vec3d targetLook = trapTarget.getRotationVec(1.0f);

        // O yönde knockback ver - fişeğin ittiği yöne ek olarak
        double strength = ElyTrapConfig.knockbackStrength;

        // Paket gönder: önce vur (interact)
        // Gerçek sunucu tarafı knockback için vanilla attack packet kullanıyoruz
        attackTarget();
    }

    /**
     * Vanilla attack paketi - sunucuya "ben hedefe vurduk" gönderir
     * Sunucu knockback hesaplar ve uygular
     */
    private static void attackTarget() {
        if (trapTarget == null || mc.interactionManager == null) return;

        // Hedef menzil içinde mi?
        double distance = mc.player.getPos().distanceTo(trapTarget.getPos());
        if (distance > 6.0) return; // Çok uzaksa vuramayız

        mc.interactionManager.attackEntity(mc.player, trapTarget);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    /**
     * Knockback paketi - hedefi belirli bir pozisyona geri iter
     * (İleri versiyonlarda velocity manipulation ile daha gelişmiş yapılabilir)
     */
    private static void sendKnockbackPacket(PlayerEntity target, Vec3d anchorPos) {
        if (target == null || mc.player == null) return;
        // Basit: hedefe vurmaya devam et (vanilla knockback zaten geri iter)
        attackTarget();
    }

    // ─────────────────────────────────────────────────────────────────
    //  YARDIMCI METODLAR
    // ─────────────────────────────────────────────────────────────────

    private static PlayerEntity findNearestPlayer() {
        if (mc.world == null || mc.player == null) return null;

        PlayerEntity nearest = null;
        double minDist = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            // Kendimizi atlıyoruz
            if (player == mc.player) continue;

            double dist = mc.player.getPos().distanceTo(player.getPos());
            if (dist < minDist && dist <= ElyTrapConfig.targetScanRadius) {
                minDist = dist;
                nearest = player;
            }
        }

        return nearest;
    }

    private static void sendMessage(String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(msg), false);
        }
    }

    // Getter'lar - HUD veya Mixin için
    public static PlayerEntity getTrapTarget() { return trapTarget; }
    public static int getFireworkKnockbackRemaining() { return fireworkKnockbackRemaining; }
    public static void addFireworkKnockback(int ticks) { fireworkKnockbackRemaining += ticks; }
}
