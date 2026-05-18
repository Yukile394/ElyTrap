package com.elytrap.handler;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;

/**
 * TrapHandler — Slot Switcher
 *
 * Önündeki oyuncu fişek bastıkça, envanterindeki
 * Knockback (Savurma) enchantlı slota geçer.
 *
 * Knockback yoksa ekrana "§cSavurma YOK" yazar.
 */
public class TrapHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Özellik açık/kapalı
    public static boolean active = false;

    // Fişek başına kaç kez slot kontrolü yapılacak (spam önlemi)
    private static int pendingSlotSwitches = 0;
    private static int switchCooldown = 0;
    private static final int SWITCH_COOLDOWN_TICKS = 2; // her 2 tik'te bir switch

    // ───────────────────────────────────────────────
    //  TOGGLE
    // ───────────────────────────────────────────────

    public static void toggle() {
        active = !active;
        pendingSlotSwitches = 0;
        switchCooldown = 0;

        if (active) {
            sendMessage("§a[SlotSwitch] §fAKTİF — Fişek algılandığında Savurma slotuna geçilecek.");
        } else {
            sendMessage("§7[SlotSwitch] §fDevre dışı.");
        }
    }

    // ───────────────────────────────────────────────
    //  HER TİK
    // ───────────────────────────────────────────────

    public static void onTick() {
        if (!active) return;
        if (mc.player == null) return;

        if (pendingSlotSwitches > 0 && switchCooldown <= 0) {
            doSlotSwitch();
            pendingSlotSwitches--;
            switchCooldown = SWITCH_COOLDOWN_TICKS;
        }

        if (switchCooldown > 0) {
            switchCooldown--;
        }
    }

    // ───────────────────────────────────────────────
    //  FİŞEK ALGILANDI — Mixin'den çağrılır
    // ───────────────────────────────────────────────

    /**
     * Her yeni fişek spawn'ında bir kez çağrılır.
     * pendingSlotSwitches artırılır; onTick işler.
     */
    public static void onFireworkDetected() {
        if (!active) return;
        pendingSlotSwitches++;
    }

    // ───────────────────────────────────────────────
    //  SLOT SWITCH MANTIĞI
    // ───────────────────────────────────────────────

    /**
     * Hotbar'da (slot 0–8) Knockback enchantı olan ilk slotu bulur ve geçer.
     * Yoksa "Savurma YOK" mesajı gönderir.
     */
    private static void doSlotSwitch() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        int knockbackSlot = findKnockbackSlot(player);

        if (knockbackSlot != -1) {
            player.getInventory().selectedSlot = knockbackSlot;
            // Sunucuya slot değişikliğini bildir
            if (mc.interactionManager != null) {
                mc.getNetworkHandler().sendPacket(
                    new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(knockbackSlot)
                );
            }
        } else {
            sendMessage("§c[SlotSwitch] Savurma YOK");
        }
    }

    /**
     * Hotbar'da (slot 0–8) Knockback (Savurma) enchantlı item arar.
     * Bulunan ilk slotun index'ini döner, bulamazsa -1.
     */
    private static int findKnockbackSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            // Enchantment map'ini al ve Knockback var mı bak
            var enchants = EnchantmentHelper.getEnchantments(stack);
            for (var entry : enchants.getEnchantments()) {
                // Knockback key: "minecraft:knockback"
                if (entry.getValue().toString().contains("knockback")) {
                    return i;
                }
            }
        }
        return -1;
    }

    // ───────────────────────────────────────────────
    //  YARDIMCI
    // ───────────────────────────────────────────────

    private static void sendMessage(String msg) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(msg), true); // action bar'da göster (true)
        }
    }
}
