package com.elytrap;

import com.elytrap.handler.TrapHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ElyTrap - Client Entrypoint
 *
 * Tuş atamaları ve tick event listener burada başlatılır.
 *
 * TUŞLAR:
 *  - V  → Trap Aç/Kapat (en yakın oyuncuyu yakala, fişek tuzağını aktif et)
 *  - B  → Kaçış Modu Aç/Kapat (biz trapa düştüysek çıkmak için)
 */
public class ElyTrapClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("ElyTrap");

    // ── Tuş Atamaları ──────────────────────────────────────────────
    public static KeyBinding KEY_TOGGLE_TRAP;
    public static KeyBinding KEY_ESCAPE_MODE;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ElyTrap] Yükleniyor...");

        // Trap toggle → V tuşu
        KEY_TOGGLE_TRAP = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytrap.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,      // ← V tuşu
                "key.categories.elytrap"
        ));

        // Kaçış modu → B tuşu
        KEY_ESCAPE_MODE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytrap.escape",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,      // ← B tuşu
                "key.categories.elytrap"
        ));

        // Her tik tuş kontrolü + mantık güncelleme
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // V basıldıysa → trap toggle
            while (KEY_TOGGLE_TRAP.wasPressed()) {
                TrapHandler.toggleTrap();
            }

            // B basıldıysa → kaçış modu toggle
            while (KEY_ESCAPE_MODE.wasPressed()) {
                TrapHandler.toggleEscapeMode();
            }

            // Her tik TrapHandler'ı güncelle
            TrapHandler.onTick();
        });

        LOGGER.info("[ElyTrap] Hazır! V = Trap, B = Kaçış Modu");
    }
}
