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
 * <li>7. 多胞碰撞代理层按 IE 模式重构（1.4.0/1.4.1，独立分支轨一）：代理 NBT 改存
 *    相对偏移（relX/relY/relZ，master = 自身 − 偏移纯函数推导）并兼容读旧绝对
 *    键；setMasterPos 校验目标位类型并去掉重入路径上的 setChanged（16.09 崩溃
 *    第三层根源）；refresh 清理范围扩到全维度；提供 /iufix cleanmultiblock
 *    一次性反污染命令。1.4.1 修正 1.4.0 代理 mixin 的 extends 写法被 mixin 0.8.5
 *    SubType$Standard.validate 拒绝（日志 "Super class ... was not found in the
 *    hierarchy of target class"，代理层重构实际从未生效）——改为 @Shadow-only
 *    无继承写法。</li>
 * <li>8. 村庄/结构生成的铁砧幽灵方块（1.4.1）：世界生成路径的 TE 经
 *    LevelChunk 构造器转移（m_142169_，只 setLevel+入表，无 onLoad），
 *    BlockEntityBase.onLoad 的 refresh 调度永远不跑 → 没有代理。修复：
 *    ChunkEvent.Load（server、仅新生成 chunk）补扫 needCollision BE 并按
 *    onLoad 同模式调度下一 tick refresh。玩家放置/磁盘重载路径自带 onLoad
 *    不受影响。1.4.1 只补了 refresh 调度、没修朝向输入，用户实测代理仍
 *    摆错位（"竖排放歪、左端嵌墙"）→ 1.4.2 补根因。</li>
 * <li>9. 村庄铁砧朝向不随 jigsaw 房屋旋转（1.4.2）：预烘焙房屋模板
 *    （metallurg_house.nbt）里铁砧 master state facing=east + BE NBT
 *    facing=east，房屋被 jigsaw 整体旋转后 vanilla 转方块状态但不转
 *    facing 属性（BlockTileEntity 无 rotate/mirror 覆写，默认原样返回），
 *    readFromNBT 又无条件读 NBT → 碰撞代理按 east 沿 Z 摆，而旋转后的
 *    墙在 Z 轴上 → 代理嵌入墙内、铁砧行与墙垂直。修复：
 *    BlockStateBase.m_60717_/m_60715_（rotate/mirror）HEAD 注入，对
 *    BlockTileEntity 系方块把 facingProperty 值旋转/镜像
 *    （mixin 0.8.5 的 InjectionInfo.findRootTargets 只匹配目标类声明方法，
 *    继承方法注入会 SelectorConstraintException 启动崩溃，故注入声明方法
 *    所在的 BlockStateBase + instanceof 过滤）；readFromNBT TAIL 把
 *    this.facing 覆写为 state facing（世界生成时 state 已旋转，先 setBlock
 *    后 loadAdditional 字节码实证）。1.4.1 的 refresh 调度保留不变，其输入
 *    朝向自此正确 → 代理落点 = 旋转后的单元格，铁砧行平行贴墙、三格全落
 *    空气，生成即正确。旧村庄存档不回溯（需重新生成），新生成即生效。</li>
 * </ol>
 */
@Mod("iufix")
public class IuFix {

    public IuFix() {
        System.out.println("[iufix] 1.4.3-hotfix3 已加载：IE 模式代理层 + 村庄铁砧世界生成路径修复 + 铁砧朝向随 jigsaw 房屋旋转（state+BE 双轨）");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(CleanMultiblockCommand.class);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ChunkLoadCollisionRefreshFix.class);
    }
}
