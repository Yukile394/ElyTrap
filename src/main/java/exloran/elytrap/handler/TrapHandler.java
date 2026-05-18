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
 *  2. Adam fişek basınca → Knockback slotuna geç.
 *  3. Adam fişeği kesince (belirli süre fişek gelmezse) → slot 1'e (index 0) dön.
 */
public class TrapHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ── Mod durumu ───────────────────────────────────────────────────────
    public static boolean active = false;

    // ── Slot switch kuyruğu ──────────────────────────────────────────────
    private static int pendingSlotSwitches = 0;
    private static int switchCooldown      = 0;
    private static final int SWITCH_COOLDOWN_TICKS = 2;

    // ── Fişek kesme tespiti ──────────────────────────────────────────────
    // Son fişek algılandıktan kaç tik sonra "kesildi" sayılsın?
    private static final int FIREWORK_STOP_TICKS = 15; // ~0.75 saniye
    private static int ticksSinceLastFirework    = FIREWORK_STOP_TICKS;
    private static boolean wasInFireworkMode     = false;

    // ── Mesafe ───────────────────────────────────────────────────────────
    private static final double RANGE = 1.2; // block

    // ─────────────────────────────────────────────────────────────────────
    //  TOGGLE
    // ─────────────────────────────────────────────────────────────────────

    public static void toggle() {
        active = !active;
        pendingSlotSwitches    = 0;
        switchCooldown         = 0;
        ticksSinceLastFirework = FIREWORK_STOP_TICKS;
        wasInFireworkMode      = false;

        if (active) {
            sendMessage("§a[SlotSwitch] §fAKTİF");
        } else {
            sendMessage("§7[SlotSwitch] §fKapalı");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HER TİK
    // ─────────────────────────────────────────────────────────────────────

    public static void onTick() {
        if (!active) return;
        if (mc.player == null) return;

        // ── Fişek durdurma sayacını artır ────────────────────────────────
        if (ticksSinceLastFirework < FIREWORK_STOP_TICKS) {
            ticksSinceLastFirework++;
        }

        // ── Fişek kesildi mi? ─────────────────────────────────────────────
        // Knockback moduna geçmiştik ve artık fişek gelmiyor → slot 1'e dön
        if (wasInFireworkMode && ticksSinceLastFirework >= FIREWORK_STOP_TICKS) {
            wasInFireworkMode   = false;
            pendingSlotSwitches = 0; // kuyruğu temizle
            switchCooldown      = 0;
            switchToSlot(0);         // slot 1 = index 0
            sendMessage("§e[SlotSwitch] §fSlot 1'e döndü");
        }

        // ── Bekleyen switch varsa işle ────────────────────────────────────
        if (pendingSlotSwitches > 0 && switchCooldown <= 0) {
            doKnockbackSwitch();
            pendingSlotSwitches--;
            switchCooldown = SWITCH_COOLDOWN_TICKS;
        }

        if (switchCooldown > 0) {
            switchCooldown--;
        }
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
        wasInFireworkMode      = true;

        // Switch kuyruğuna ekle
        pendingSlotSwitches++;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  KNOCKBACK SLOTUNA GEÇ
    // ─────────────────────────────────────────────────────────────────────

    private static void doKnockbackSwitch() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        int slot = findKnockbackSlot(player);
        if (slot != -1) {
            switchToSlot(slot);
        } else {
            sendMessage("§c[SlotSwitch] Savurma YOK");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SLOT DEĞİŞTİR (paket ile sunucuya bildir)
    // ─────────────────────────────────────────────────────────────────────

    private static void switchToSlot(int index) {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        player.getInventory().selectedSlot = index;

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
