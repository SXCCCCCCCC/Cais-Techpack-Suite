package com.sxcccccccc.iufix.mixin;

import com.denfop.api.pollution.PollutionManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 污染"开着（有环境表现）但不给玩家负面效果"。
 * <p>
 * {@code PollutionManager.work(Player)} 是污染唯一 debuff 施加点（PlayerTickEvent 驱动，
 * 每 200 tick）：土壤≥1 级给缓慢、≥2 挖掘疲劳、≥3 虚弱、≥4 恶心；空气≥2 恶心、≥3 失明、
 * ≥4 毒气（IUPotion.poison_gas），共 7 处 {@code player.addEffect}，方法体无其他功能
 * （javap 发布 jar 字节码确认全方法只做效果施加）。
 * <p>
 * HEAD 取消 = 玩家永不获得任何污染 debuff；污染值产生/扩散/地形退化/煤烟/毒藤疯长/
 * 客户端雾、天空、屏幕遮罩等全部照常运行（依赖 airPollution/soilPollution 配置，不受影响）。
 */
@Mixin(value = PollutionManager.class)
public class PollutionDebuffDisableMixin {

    @Inject(method = "work", at = @At("HEAD"), cancellable = true, remap = false)
    private void iucore$disablePollutionDebuffs(Player player, CallbackInfo ci) {
        ci.cancel();
    }
}
