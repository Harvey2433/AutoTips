package me.maple_bamboo_team.autotips.client;

import me.maple_bamboo_team.autotips.LowHpWarningManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class AutotipsClient implements ClientModInitializer {

    public static final Identifier WARNING_TEXTURE = Identifier.of("autotips", "textures/gui/red-warn.png");
    private static final int BASE_ICON_SIZE = 32;
    private static final int TARGET_ICON_SIZE = BASE_ICON_SIZE / 2; // 16x16
    private static final int CROSSHAIR_Y_OFFSET = 20; // 准星下方偏移量 (像素)

    // --- 音效常量：更改为 竖琴 (Harp) 并修复 Identifier 构造错误 ---
    // 错误修复：使用 Identifier.of() 静态工厂方法代替私有构造函数
    public static final Identifier NOTE_HARP_ID = Identifier.of("minecraft", "block.note_block.harp");
    private static final float LOWEST_PITCH = 2.0F; // 对应音符盒最高音

    // 新增：播放警告音效的静态方法
    public static void playWarningSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.getSoundManager() == null) {
            return;
        }

        // 使用竖琴音效 ID 和最低音调
        client.getSoundManager().play(
                PositionedSoundInstance.master(
                        SoundEvent.of(NOTE_HARP_ID), // 竖琴音效 ID
                        LOWEST_PITCH,
                        0.55F                         // 音量
                )
        );
    }

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(LowHpWarningManager::tick);

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            if (LowHpWarningManager.shouldRender()) {

                int screenWidth = guiGraphics.getScaledWindowWidth();
                int screenHeight = guiGraphics.getScaledWindowHeight();

                // 计算准星偏下位置
                int x = (screenWidth / 2) - (TARGET_ICON_SIZE / 2);
                int y = (screenHeight / 2) + CROSSHAIR_Y_OFFSET;

                // 绘制主图标
                guiGraphics.drawTexture(
                        WARNING_TEXTURE,
                        x, y,
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