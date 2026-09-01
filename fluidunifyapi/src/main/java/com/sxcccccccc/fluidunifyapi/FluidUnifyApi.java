package com.sxcccccccc.fluidunifyapi;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * fluidunifyapi：可配置的"机器流体输入定义操纵" API（1.20.1-Tec 包自用）。
 *
 * <p>一句话定位：对 IC2 Refabricated / Industrial Foregoing / Industrial Upgrade
 * 每一台有流体输入的机器，提供 KubeJS 配置驱动的新流体接入能力——只改输入定义，
 * 输出侧、流体身份、罐防混装语义一概不碰。取代旧 fluidunifyfix / iuunify 的
 * "罐门禁/匹配宽限"路线（后者动 Forge FluidTank.fill 语义，已被否决）。</p>
 *
 * <p>架构三层：</p>
 * <ul>
 *   <li>配置层（{@code kubejs.FluidUnifyEvents}）：KubeJS 插件注册自定义事件，
 *       脚本按六键形态声明补丁；</li>
 *   <li>核心引擎（{@code core.UnifiedFluidRegistry}）：补丁解析/校验/查询中枢，
 *       幂等、无锁读；</li>
 *   <li>机器适配（iu/ifmod/ic2 三包）：IU 配方族 = 公开 API 写运行时配方表；
 *       IU/IF 硬编码族 = 谓词咽喉点 mixin 探针咨询；IC2 = 逐机判定点 mixin；
 *       IF datapack 族 = KubeJS 配方管线（服务端直写 + 客户端 injectRuntimeRecipes）。</li>
 * </ul>
 *
 * <p>依赖（全部 compileOnly，运行时由包内既有 mod 提供）：IU 3.4.0.10、Titanium
 * 3.8.35、kjsindustrialforegoing 1.0、KubeJS 2001.6.5 + rhino + architectury、
 * JEI 15.48.0.179、IC2 Refabricated 0.6。</p>
 */
@Mod("fluidunifyapi")
public class FluidUnifyApi {

    public static final Logger LOGGER = LogUtils.getLogger();

    public FluidUnifyApi() {
        LOGGER.info("[fluidunifyapi] loaded — machine fluid input manipulation API (IC2/IF/IU)");
    }
}
