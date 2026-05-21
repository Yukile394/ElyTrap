package com.elytrap.handler;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

/**
 * TrapHandler — Slot Switcher
 *
 * Kurallar:
 *  1. Mod aktifken sadece 1.2 block ilerideki oyuncuya tepki ver.
 *  2. Adam fişek basınca → Knockback slotuna ANINDA geç.
 *  3. Adam fişeği kesince → slot 1'e (index 0) ANINDA dön.
 */
public class TrapHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ── Mod durumu ───────────────────────────────────────────────────────
    public static boolean active = false;

    // ── Fişek kesme tespiti ──────────────────────────────────────────────
    // Kaç tik fişek gelmezse "kesildi" sayılsın? (3 tick = ~150ms, en hızlı güvenli değer)
    private static final int FIREWORK_STOP_TICKS = 3;
    private static int ticksSinceLastFirework    = FIREWORK_STOP_TICKS;
    private static boolean wasInFireworkMode     = false;

    // ── Önceki slot (kendi değiştirmesine izin vermek için) ──────────────
    private static int lastKnownSlot = -1;
    private static boolean weDidSwitch = false;

    // ── Mesafe ───────────────────────────────────────────────────────────
    private static final double RANGE = 1.2; // block

    // ─────────────────────────────────────────────────────────────────────
    //  TOGGLE
    // ─────────────────────────────────────────────────────────────────────

    public static void toggle() {
        active = !active;
        ticksSinceLastFirework = FIREWORK_STOP_TICKS;
        wasInFireworkMode      = false;
        weDidSwitch            = false;
        lastKnownSlot          = (mc.player != null) ? mc.player.getInventory().selectedSlot : -1;

        if (active) {
            sendMessage("§a[ElyTRAP] §fAktif");
        } else {
            sendMessage("§7[ElyTRAP] §fKapalı");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HER TİK
    // ─────────────────────────────────────────────────────────────────────

    public static void onTick() {
        if (!active) return;
        if (mc.player == null) return;

        int currentSlot = mc.player.getInventory().selectedSlot;

        // ── Oyuncu KENDİ slot değiştirdiyse, biz yapmadıysak takip et ────
        if (!weDidSwitch && lastKnownSlot != -1 && currentSlot != lastKnownSlot) {
            // Oyuncunun kendi değişikliği — wasInFireworkMode'u koru ama
            // son slotu güncelle (slot dönüşleri oradan olmaz)
        }
        weDidSwitch = false;

        // ── Fişek durdurma sayacını artır ────────────────────────────────
        if (ticksSinceLastFirework < FIREWORK_STOP_TICKS) {
            ticksSinceLastFirework++;
        }

        // ── Fişek kesildi mi? → ANINDA slot 1'e dön ─────────────────────
        if (wasInFireworkMode && ticksSinceLastFirework >= FIREWORK_STOP_TICKS) {
            wasInFireworkMode = false;
            switchToSlot(0); // slot 1 = index 0 — ANINDA
            sendMessage("§e[ElyTRAP] §fSlot 1'e döndü");
        }

        lastKnownSlot = mc.player.getInventory().selectedSlot;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  FİŞEK ALGILANDI — Mixin'den çağrılır
    // ─────────────────────────────────────────────────────────────────────

    public static void onFireworkDetected(PlayerEntity shooter) {
        if (!active) return;
        if (mc.player == null) return;

        // ── Mesafe kontrolü: sadece 1.2 block içindeyse tepki ver ─────────
        double dist = mc.player.getPos().distanceTo(shooter.getPos());
        if (dist > RANGE) return;

        // Sayacı sıfırla (fişek hâlâ devam ediyor)
        ticksSinceLastFirework = 0;

        // İlk kez fişek algılandıysa ANINDA geç
        if (!wasInFireworkMode) {
            wasInFireworkMode = true;
            doKnockbackSwitch(); // ANINDA — bekleme yok
        } else {
            // Zaten knockback modundayken tekrar fişek geldi: sayacı sıfırladık, geçiş yok
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  KNOCKBACK SLOTUNA GEÇ — ANINDA
    // ─────────────────────────────────────────────────────────────────────

    private static void doKnockbackSwitch() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        int slot = findKnockbackSlot(player);
        if (slot != -1) {
            switchToSlot(slot);
        } else {
            sendMessage("§c[ElyTRAP] Savurma YOK");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SLOT DEĞİŞTİR — Anında, paket ile sunucuya bildir
    // ─────────────────────────────────────────────────────────────────────

    private static void switchToSlot(int index) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        if (player.getInventory().selectedSlot == index) return; // Zaten bu slottaysa işlem yapma

        weDidSwitch = true;
        player.getInventory().selectedSlot = index;
        lastKnownSlot = index;

        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(
                new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(index)
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  KNOCKBACK ENCHANTLI SLOT BUL (hotbar 0–8)
    // ─────────────────────────────────────────────────────────────────────

    private static int findKnockbackSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants == null) continue;

            for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                if (entry.getKey().isPresent()) {
                    String id = entry.getKey().get().getValue().toString();
                    if (id.contains("knockback")) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  YARDIMCI
    // ─────────────────────────────────────────────────────────────────────

    private static void sendMessage(String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(msg), true); // action bar
        }
    }
}
