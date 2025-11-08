package me.maple_bamboo_team.autotips.client;

import me.maple_bamboo_team.autotips.LowHpWarningManager;
import me.maple_bamboo_team.autotips.LowArmorWarningManager; // 导入盔甲管理器

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class AutotipsClient implements ClientModInitializer {

    // --- 纹理定义 ---
    public static final Identifier WARNING_TEXTURE = Identifier.of("autotips", "textures/gui/red-warn.png");
    // 新增：黄色盔甲警告图标
    public static final Identifier ARMOR_WARNING_TEXTURE = Identifier.of("autotips", "textures/gui/yellow-warn.png");

    private static final int BASE_ICON_SIZE = 32;
    private static final int TARGET_ICON_SIZE = BASE_ICON_SIZE / 2; // 16x16
    private static final int CROSSHAIR_Y_OFFSET = 20; // 血量图标偏移量 (准星下方 20)
    // 新增：盔甲图标相对于血量图标的偏移量
    private static final int ARMOR_ICON_OFFSET = TARGET_ICON_SIZE + 4;

    // --- 音效常量 ---
    public static final Identifier NOTE_HARP_ID = Identifier.of("minecraft", "block.note_block.harp");
    private static final float LOWEST_PITCH = 1.781797F;

    public static void playWarningSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.getSoundManager() == null) {
            return;
        }

        client.getSoundManager().play(
                PositionedSoundInstance.master(
                        SoundEvent.of(NOTE_HARP_ID),
                        LOWEST_PITCH,
                        0.6F
                )
        );
    }

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(LowHpWarningManager::tick);
        // 注册新的盔甲管理器 tick
        ClientTickEvents.END_CLIENT_TICK.register(LowArmorWarningManager::tick);

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            int screenWidth = guiGraphics.getScaledWindowWidth();
            int screenHeight = guiGraphics.getScaledWindowHeight();

            // 计算血量图标的基准位置
            int baseX = (screenWidth / 2) - (TARGET_ICON_SIZE / 2);
            int baseY = (screenHeight / 2) + CROSSHAIR_Y_OFFSET;

            // --- 1. 绘制血量警告图标 (红色) ---
            if (LowHpWarningManager.shouldRender()) {
                guiGraphics.drawTexture(
                        WARNING_TEXTURE,
                        baseX, baseY,
                        0, 0,
                        TARGET_ICON_SIZE,
                        TARGET_ICON_SIZE,
                        TARGET_ICON_SIZE,
                        TARGET_ICON_SIZE
                );
            }

            // --- 2. 绘制盔甲警告图标 (黄色) ---
            if (LowArmorWarningManager.shouldRender()) {

                // 位置：血量图标的 Y 坐标 + 血量图标高度 + 额外偏移
                int armorY = baseY + ARMOR_ICON_OFFSET;

                guiGraphics.drawTexture(
                        ARMOR_WARNING_TEXTURE, // 使用黄色纹理
                        baseX, armorY,         // X 坐标与血量图标相同
                        0, 0,
                        TARGET_ICON_SIZE,
                        TARGET_ICON_SIZE,
                        TARGET_ICON_SIZE,
                        TARGET_ICON_SIZE
                );
            }
        });
    }
}