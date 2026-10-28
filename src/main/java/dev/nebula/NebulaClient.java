package dev.nebula;

import dev.nebula.core.Commands;
import dev.nebula.core.Config;
import dev.nebula.core.Mod;
import dev.nebula.core.ModManager;
import dev.nebula.modules.*;
import dev.nebula.ui.ClickGui;
import dev.nebula.ui.Hud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NebulaClient implements ClientModInitializer {
    public static final String NAME = "Nebula";
    public static final String VERSION = "1.0.0";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);

    public static int guiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;
    /** Set by the GUI when it closes via the GUI key so the tick handler doesn't instantly reopen it. */
    public static boolean guiWasDown;

    @Override
    public void onInitializeClient() {
        CombatMods.init();
        MovementMods.init();
        RenderMods.init();
        PlayerMods.init();
        MiscMods.init();
        HudMods.init();
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(NebulaClient::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Hud.render(ctx));
        ClientSendMessageEvents.ALLOW_CHAT.register(msg -> !Commands.handle(msg));
        ClientSendMessageEvents.MODIFY_CHAT.register(msg -> {
            if (ModManager.isOn("ChatSuffix") && msg.length() < 240) return msg + " | Nebula";
            return msg;
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Config.save());
        LOG.info("{} {} loaded with {} modules", NAME, VERSION, ModManager.all().size());
    }

    private static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.getWindow() == null) return;
        long h = mc.getWindow().getHandle();

        boolean guiDown = InputUtil.isKeyPressed(h, guiKey);
        if (guiDown && !guiWasDown && mc.currentScreen == null) mc.setScreen(new ClickGui());
        guiWasDown = guiDown;

        for (Mod m : ModManager.all()) {
            if (m.key != -1) {
                boolean down = mc.currentScreen == null && InputUtil.isKeyPressed(h, m.key);
                if (down && !m.wasDown) m.toggle();
                m.wasDown = down;
            }
            if (m.isEnabled()) m.tickSafe();
        }
        Hud.tick();
    }
}
