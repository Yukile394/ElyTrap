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
 * ElyTrapClient — Client Entrypoint
 *
 * TEK TUŞ:
 *  V → SlotSwitch Aç/Kapat
 *
 * Açıkken: Önündeki oyuncu fişek bastıkça,
 * hotbar'daki Knockback (Savurma) enchantlı slota geçilir.
 * Knockback yoksa action bar'da "Savurma YOK" yazar.
 */
public class ElyTrapClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("ElyTrap");

    public static KeyBinding KEY_TOGGLE;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ElyTrap] Yükleniyor...");

        // V tuşu → SlotSwitch toggle
        KEY_TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytrap.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.elytrap"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KEY_TOGGLE.wasPressed()) {
                TrapHandler.toggle();
            }
            TrapHandler.onTick();
        });

        LOGGER.info("[ElyTrap] Hazır! V = SlotSwitch Aç/Kapat");
    }
}
