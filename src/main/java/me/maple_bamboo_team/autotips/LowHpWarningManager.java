package me.maple_bamboo_team.autotips;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import me.maple_bamboo_team.autotips.client.AutotipsClient;

public class LowHpWarningManager {

    // --- 阈值定义 ---
    private static final float DEFAULT_CRITICAL_THRESHOLD = 9.0f;
    private static final float ABSORPTION_CRITICAL_THRESHOLD = 6.0f;

    // --- 频率定义 ---
    private static final int FLASH_SPEED_TICKS = 2; // 图标闪烁频率 (2 刻周期)
    private static final int SOUND_PLAY_INTERVAL = 2; // 音效播放间隔 (4 刻周期)

    // --- 声音持续时间 (3.7 秒 * 20 刻/秒 = 74 刻) ---
    private static final int SOUND_DURATION_TICKS = (int)(1.6 * 20);

    // --- 状态变量 ---
    private static float lastHealth = -1.0f;
    private static int flashTimer = 0;
    private static boolean isFlashing = false;
    private static int soundTickCounter = 0; // 控制音效播放间隔
    private static int soundDurationTimer = 0; // 新增：控制音效总播放时长
    private static boolean soundActive = false; // 新增：指示声音是否仍在播放期

    /**
     * 在客户端 Tick 结束时调用，用于更新状态。
     */
    public static void tick(MinecraftClient client) {
        PlayerEntity player = client.player;

        if (player == null || player.isDead()) {
            isFlashing = false;
            lastHealth = -1.0f;
            soundTickCounter = 0;
            soundDurationTimer = 0;
            soundActive = false; // 死亡时重置声音状态
            return;
        }

        float currentHealth = player.getHealth();

        // --- 1. 确定当前生效的阈值 ---
        float effectiveThreshold = DEFAULT_CRITICAL_THRESHOLD;
        if (player.hasStatusEffect(StatusEffects.ABSORPTION)) {
            effectiveThreshold = ABSORPTION_CRITICAL_THRESHOLD;
        }

        if (lastHealth < 0) {
            lastHealth = currentHealth;
            return;
        }

        // --- 2. 立即取消检查 (高于当前阈值立即取消显示) ---
        if (currentHealth > effectiveThreshold) {
            isFlashing = false;
            flashTimer = 0;
            soundTickCounter = 0;

            // 🚨 关键：闪烁停止时，重置声音计时器和状态
            soundDurationTimer = 0;
            soundActive = false;

            lastHealth = currentHealth;
            return;
        }

        // --- 3. 闪烁触发和维持逻辑 ---
        boolean healthDecreased = currentHealth < lastHealth;

        if (currentHealth <= effectiveThreshold) {

            if (isFlashing) {
                // 如果已在闪烁，维持闪烁状态
            } else if (healthDecreased) {
                // 如果血量下降且未在闪烁，则开始闪烁
                isFlashing = true;
                // 首次开始闪烁时，启动声音计时器
                soundActive = true;
                soundDurationTimer = 0; // 确保计时器归零，开始新的 3.7s 计时
            }
        }

        // --- 4. 声音持续时间更新 ---
        if (soundActive) {
            soundDurationTimer++;
            if (soundDurationTimer >= SOUND_DURATION_TICKS) {
                // 3.7 秒时间到，停止声音播放，但 soundActive 保持 true 直到 isFlashing 结束
                soundActive = false;
            }
        }

        // --- 5. 闪烁周期更新 ---
        if (isFlashing) {

            // 📢 音效播放逻辑：只有在 soundActive 状态下才播放
            if (soundActive) {
                soundTickCounter++;
                if (soundTickCounter >= SOUND_PLAY_INTERVAL) {
                    AutotipsClient.playWarningSound();
                    soundTickCounter = 0;
                }
            } else {
                // 声音停止后，重置间隔计时器
                soundTickCounter = 0;
            }

            // 图标闪烁频率 (保持 2 刻周期)
            flashTimer++;
            if (flashTimer >= FLASH_SPEED_TICKS * 2) {
                flashTimer = 0;
            }
        } else {
            flashTimer = 0;
            soundTickCounter = 0;
            soundDurationTimer = 0; // 冗余：确保 soundActive 为 false 时，计时器归零
            soundActive = false;
        }

        // --- 6. 更新上次生命值 ---
        lastHealth = currentHealth;
    }

    /**
     * 用于渲染时判断是否应该显示图标。
     */
    public static boolean shouldRender() {
        if (!isFlashing) {
            return false;
        }
        return flashTimer < FLASH_SPEED_TICKS;
    }
}