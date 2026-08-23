package com.sxcccccccc.iufix;

import net.minecraftforge.fml.common.Mod;

/**
 * Industrial Upgrade 3.4.0.10 五处 bug 修复（本包自用 fix mod，mixin 全部注入 denfop 类）。
 *
 * <p>1. {@code BlockTileEntity.canHarvestBlock} Wrench 分支检查不存在的 {@code minecraft:wrench}
 *    tag → 49 个 Wrench 采集类方块任何工具打爆都不掉落。修复：@Redirect 该处
 *    {@code MultiBlockEntity.getHarvestTool()} 调用，Wrench → Pickaxe，一处覆盖全部子类。
 *    （iucore 的 IUHarvestToolMixin 只在 {@code create} 里重定向，不影响 canHarvestBlock 的
 *    switch 判定，对本 bug 无效——本 mod 直接修门控点。）</li>
 * <li>2. {@code ComponentProcess} 3 个方法 7 处 {@code invSlotRecipes.getTank()} 裸调无 null 守卫
 *    （checkFluidRecipe ×3、updateEntityServer ×3、operateWithMax(MachineRecipe,int) ×1）。
 *    修复：@Redirect 全部调用点为安全取值，null → 反射构造的空罐（getFluidAmount=0）。</li>
 * <li>3. {@code TileEntityRenderApiary} 静态 register 标志 + 实例字段 flower：资源重载/换世界后
 *    新实例 flower=null 裸调渲染崩溃。修复：renderItem HEAD 取消 null/空栈。</li>
 * <li>4. {@code BlockFluidIU.drain} 是桩实现恒返 {@code FluidStack.EMPTY}，流体方块不可抽取。
 *    修复：HEAD 注入真实抽取（1000mb + SIMULATE 不清源 / EXECUTE 清源块）。</li>
 * <li>5. {@code ScreenScanner}/{@code ScreenVeinSensor} 的 mouseClicked 无守卫，widget 层点击
 *    错位引发 AIOOBE/NPE。修复：右键时若图案格（Scanner）/空地图或空 vector（VeinSensor）
 *    则取消本次点击。</li>
 * <li>6. 污染"开着但无玩家负面效果"：{@code PollutionManager.work} 是唯一 debuff 施加点
 *    （缓慢/挖掘疲劳/虚弱/恶心/失明/毒气共 7 处 addEffect，方法体无其他功能）。
 *    修复：HEAD 取消该方法，污染数值/扩散/地形与环境渲染照常。</li>
 * <li>7. 多胞碰撞代理层按 IE 模式重构（1.4.0，独立分支轨一）：代理 NBT 改存
 *    相对偏移（relX/relY/relZ，master = 自身 − 偏移纯函数推导）并兼容读旧绝对
 *    键；setMasterPos 校验目标位类型并去掉重入路径上的 setChanged（16.09 崩溃
 *    第三层根源）；refresh 清理范围扩到全维度；提供 /iufix cleanmultiblock
 *    一次性反污染命令。</li>
 * </ol>
 */
@Mod("iufix")
public class IuFix {

    public IuFix() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(CleanMultiblockCommand.class);
    }
}
