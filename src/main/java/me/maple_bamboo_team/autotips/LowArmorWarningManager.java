package me.maple_bamboo_team.autotips;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ArmorItem;

public class LowArmorWarningManager {

    // --- 缺失警告定义 ---
    private static final int MIN_ARMOR_COUNT = 3;
    private static final int FLASH_DURATION_TICKS = 5 * 20;
    private static final int FLASH_SPEED_TICKS = 2;

    // --- 耐久度警告定义 ---
    private static final double WARNING_THRESHOLD_RATIO = 0.55;

    // --- 核心全局状态 ---
    private static boolean isLogicEnabled = false; // 逻辑是否已启用 (必须穿戴4件)

    // --- 缺失警告状态 ---
    private static boolean isMissingWarningActive = false;
    private static int missingFlashTimer = 0;
    private static int missingDurationTimer = 0;

    // --- 耐久度警告状态 ---
    private static boolean isDamageWarningActive = false;
    private static int lastArmorTotalDamage = 0;


    private static void resetAllStates() {
        // 玩家死亡/退出/进入世界时的重置操作
        isLogicEnabled = false;
        isMissingWarningActive = false;
        missingDurationTimer = 0;
        missingFlashTimer = 0;
        isDamageWarningActive = false;
        lastArmorTotalDamage = 0;

        // 确保在 resetAllStates 之外，当 player == null 时也会调用此方法，以覆盖死亡/退出情况。
    }

    /**
     * 在客户端 Tick 结束时调用，用于更新状态。
     */
    public static void tick(MinecraftClient client) {
        // 玩家不存在（退出）或死亡后，必须重置所有状态并退出
        if (client.player == null || (client.player != null && client.player.isDead())) {
            resetAllStates();
            return;
        }

        PlayerEntity player = client.player;
        int equippedArmorCount = 0;
        boolean isAnyArmorDamaged = false;
        int currentTotalDamage = 0;

        // 遍历盔甲槽位
        for (ItemStack itemStack : player.getArmorItems()) {
            if (!itemStack.isEmpty() && itemStack.getItem() instanceof ArmorItem) {
                equippedArmorCount++;

                int maxDamage = itemStack.getMaxDamage();
                int currentDamage = itemStack.getDamage();
                currentTotalDamage += currentDamage;

                if ((double)currentDamage / maxDamage >= WARNING_THRESHOLD_RATIO) {
                    isAnyArmorDamaged = true;
                }
            }
        }

        // --- 0. 逻辑启用检查 (核心修改) ---
        if (!isLogicEnabled) {
            // 只有当玩家首次穿戴了全部 4 件盔甲时，才启用逻辑
            if (equippedArmorCount == 4) {
                isLogicEnabled = true;
                // 注意：这里启用了逻辑，但不会在当前刻触发警告，
                // 因为后续的触发依赖于耐久度发生变化（下降）。
            } else {
                // 逻辑未启用，且玩家未穿满 4 件盔甲，不执行警告检测
                lastArmorTotalDamage = 0; // 重置上次状态，防止启用后立即触发
                return;
            }
        }

        // --- 以下警告逻辑仅在 isLogicEnabled == true 时执行 ---

        // ----------------------------------------------------
        // --- 1. 盔甲缺失警告 (优先级高) ---
        // ----------------------------------------------------

        if (equippedArmorCount < MIN_ARMOR_COUNT) {
            // ... (缺失警告逻辑保持不变)
            if (!isMissingWarningActive) {
                isMissingWarningActive = true;
                missingDurationTimer = 0;
                missingFlashTimer = 0;
            }

            if (isMissingWarningActive && missingDurationTimer < FLASH_DURATION_TICKS) {
                missingDurationTimer++;
                missingFlashTimer++;
                if (missingFlashTimer >= FLASH_SPEED_TICKS * 2) {
                    missingFlashTimer = 0;
                }
            } else if (isMissingWarningActive && missingDurationTimer >= FLASH_DURATION_TICKS) {
                missingFlashTimer = 0;
            }

        } else {
            // 盔甲数量充足 (>= 3 件)
            if (isMissingWarningActive) {
                isMissingWarningActive = false;
                missingDurationTimer = 0;
                missingFlashTimer = 0;
            }
        }

        // ----------------------------------------------------
        // --- 2. 盔甲耐久度警告 (如果缺失警告未激活，才检查耐久度) ---
        // ----------------------------------------------------
        if (!isMissingWarningActive) {

            boolean damageChanged = currentTotalDamage != lastArmorTotalDamage;

            if (isAnyArmorDamaged) {
                if (!isDamageWarningActive && damageChanged) {
                    isDamageWarningActive = true;
                }
            } else {
                isDamageWarningActive = false;
            }

        } else {
            isDamageWarningActive = false;
        }

        // --- 3. 更新上次状态 ---
        lastArmorTotalDamage = currentTotalDamage;
    }

    // ... (isLogicReady() 和 shouldRender() 保持不变)

    public static boolean isLogicReady() {
        return isLogicEnabled;
    }

    public static boolean shouldRender() {
        if (!isLogicEnabled) {
            return false;
        }

        if (isMissingWarningActive) {
            if (missingDurationTimer < FLASH_DURATION_TICKS) {
                return missingFlashTimer < FLASH_SPEED_TICKS;
            } else {
                return true;
            }
        }

        return isDamageWarningActive;
    }
}