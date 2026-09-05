package com.sxcccccccc.iufix.mixin;

import com.denfop.api.Recipes;
import com.denfop.api.recipe.BaseMachineRecipe;
import com.denfop.api.recipe.Input;
import com.denfop.api.recipe.InventoryRecipes;
import com.denfop.api.recipe.MachineRecipe;
import com.denfop.api.recipe.RecipeOutput;
import com.denfop.blockentity.base.BlockEntityMolecularTransformer;
import com.denfop.containermenu.ContainerMenuBaseMolecular;
import com.denfop.recipe.IInputHandler;
import com.denfop.recipe.IInputItemStack;
import com.denfop.screen.ScreenIndustrialUpgrade;
import com.denfop.utils.Localization;
import com.denfop.utils.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 1.9.2/1.9.3：分子重组机 GUI 渲染 NPE 防线（crash-2026-09-05_09.49.29 / _11.01.41）。
 *
 * <p>根因：ScreenMolecularTransformer 的 queue(x64) 渲染分支裸调
 * {@code getOutput(i).getRecipe()} 且先读字段 {@code output[i]} 再解引用。
 * {@code getOutput} = 实时配方查表（{@code output[i] = inputSlot[i].process()}），
 * 无命中时合法返回 null，且会把这个 null **写回 output[i]**；而客户端 BE 的
 * 槽内容/缓存同步与渲染帧之间存在并发窗口（11:01 崩溃实锤：守卫
 * continue_proccess 判完缓存非空后、handler 读取前，缓存被另一线程
 * setRecipeOutput(null) 清掉）。1.9.2 的"回退缓存"方案因此可能拿到 null
 * 再被上层解引用——仍崩。
 *
 * <p>1.9.3 双轨防线：
 * <ul>
 * <li>m_7286_（渲染体，两次崩溃现场）：@Overwrite 整体替换，原逻辑等价复刻
 * （super.m_7286_ → renderBackground；this.draw → drawString 内联；
 * guiLeft/guiTop → getGuiLeft/getGuiTop），整个方法包 try-catch(Throwable)，
 * 任何异常（含竞态 NPE）拦截 + 节流诊断日志，渲染帧放弃本次绘制不崩游戏。</li>
 * <li>drawForegroundLayer（工具提示层，同源竞态）：保留原方法（super 调用
 * 不动），两个 @Redirect 堵死窗口——GETFIELD output 重定向返回"非空安全
 * 数组"（null 元素换一次性假配方），getOutput 调用重定向：实时命中→直通，
 * null→缓存（命中打诊断），缓存也 null→假配方兜底，绝不返回 null。</li>
 * </ul>
 */
@Mixin(targets = "com.denfop.screen.ScreenMolecularTransformer", remap = false)
public abstract class ScreenMolecularTransformerGuardMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("iufix");

    /** 节流：相同签名 5 秒内不重复打，防止 GUI 常驻期间逐帧刷屏。 */
    private static long lastLogNanos;
    private static String lastLogSignature;

    /** 兜底假配方：仅在实时与缓存都 null 的竞态帧返回给显示代码，永不参与匹配。 */
    private static MachineRecipe dummyRecipe;

    /** 目标类自声明字段（运行时名不变，remap=false 字面匹配）。 */
    @Shadow
    public ContainerMenuBaseMolecular container;

    /** leftPos/topPos（AbstractContainerScreen protected，@Shadow 不爬父链）→ 运行时反射。 */
    private static final Field GUI_LEFT = iufix$findGuiField("f_97735_", "leftPos");
    private static final Field GUI_TOP = iufix$findGuiField("f_97736_", "topPos");

    private static Field iufix$findGuiField(String runtimeName, String devName) {
        for (String name : new String[]{runtimeName, devName}) {
            try {
                Field field = AbstractContainerScreen.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Exception ignored) {
                // try next name
            }
        }
        return null;
    }

    private int iufix$guiLeft() {
        try {
            return GUI_LEFT == null ? 0 : GUI_LEFT.getInt(this);
        } catch (Exception e) {
            return 0;
        }
    }

    private int iufix$guiTop() {
        try {
            return GUI_TOP == null ? 0 : GUI_TOP.getInt(this);
        } catch (Exception e) {
            return 0;
        }
    }

    /** ScreenIndustrialUpgrade 的 public 方法：mixin 类非其子类，转型调用。 */
    private void iufix$bindTexture() {
        ((ScreenIndustrialUpgrade) (Object) this).bindTexture();
    }

    private void iufix$drawTexturedModalRect(GuiGraphics poseStack, int i, int i1, int i2, int i3, int i4, int i5) {
        ((ScreenIndustrialUpgrade) (Object) this).drawTexturedModalRect(poseStack, i, i1, i2, i3, i4, i5);
    }

    private void iufix$renderBackground(GuiGraphics poseStack) {
        ((Screen) (Object) this).renderBackground(poseStack);
    }

    @Overwrite
    protected void m_7286_(GuiGraphics poseStack, float f, int x, int y) {
        try {
            iufix$renderBackground(poseStack);
            iufix$bindTexture();
            String input = Localization.translate("gui.MolecularTransformer.input") + ": ";
            String output = Localization.translate("gui.MolecularTransformer.output") + ": ";
            String energyPerOperation = Localization.translate("gui.MolecularTransformer.energyPerOperation") + ": ";
            String progress = Localization.translate("gui.MolecularTransformer.progress") + ": ";
            double chargeLevel = 20.0 * this.container.base.getProgress(0);
            if (this.container.base.maxAmount == 1) {
                if (!this.container.base.queue) {
                    iufix$drawTexturedModalRect(poseStack, iufix$guiLeft() + 22, iufix$guiTop() + 30, 240, 20, 16, 11);
                } else {
                    iufix$drawTexturedModalRect(poseStack, iufix$guiLeft() + 42, iufix$guiTop() + 30, 240, 20, 16, 11);
                }
            } else if (!this.container.base.queue) {
                iufix$drawTexturedModalRect(poseStack, iufix$guiLeft() + 22, iufix$guiTop() + 30, 27, 244, 16, 11);
            } else {
                iufix$drawTexturedModalRect(poseStack, iufix$guiLeft() + 42, iufix$guiTop() + 30, 27, 244, 16, 11);
            }

            if (this.container.base.maxAmount == 1) {
                if (chargeLevel > 0.0
                        && !this.container.base.inputSlot[0].isEmpty()
                        && this.container.base.inputSlot[0].continue_proccess(this.container.base.outputSlot[0])) {
                    MachineRecipe output1 = this.container.base.output[0];
                    iufix$bindTexture();
                    iufix$drawTexturedModalRect(poseStack, iufix$guiLeft() + 23, iufix$guiTop() + 75, 242, 32, 14, (int) chargeLevel);
                    if (!this.container.base.queue) {
                        this.drawText(poseStack,
                                input + ModUtils.cleanComponentString(this.container.base.inputSlot[0].get(0).getDisplayName().getString()),
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 55,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        this.drawText(poseStack,
                                output + ModUtils.cleanComponentString(output1.getRecipe().output.items.get(0).getDisplayName().getString()),
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 65,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        this.drawText(poseStack,
                                energyPerOperation + ModUtils.getString(this.container.base.energySlots[0]) + " EF",
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 75,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        if (this.container.base.getProgress(0) * 100.0 <= 100.0) {
                            this.drawText(poseStack,
                                    progress + (this.container.base.getProgress(0) <= 0.01 ? 0 : ModUtils.getString(this.container.base.getProgress(0) * 100.0)) + "%",
                                    iufix$guiLeft() + 60,
                                    iufix$guiTop() + 85,
                                    ModUtils.convertRGBcolorToInt(255, 255, 255)
                            );
                        }

                        this.drawText(poseStack,
                                "EF/t: " + ModUtils.getString(this.container.base.perenergy),
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 95,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        double hours = 0.0;
                        double minutes = 0.0;
                        double seconds = 0.0;
                        List<Double> time = this.container.base.getTime(0);
                        if (time.size() > 0) {
                            hours = time.get(0);
                            minutes = time.get(1);
                            seconds = time.get(2);
                        }

                        String time1 = hours > 0.0 ? ModUtils.getString(hours) + Localization.translate("iu.hour") : "";
                        String time2 = minutes > 0.0 ? ModUtils.getString(minutes) + Localization.translate("iu.minutes") : "";
                        String time3 = seconds > 0.0 ? ModUtils.getString(seconds) + Localization.translate("iu.seconds") : "";
                        this.drawText(poseStack,
                                Localization.translate("iu.timetoend") + time1 + time2 + time3,
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 105,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                    } else if (this.container.base.outputSlot[0].get(0).isEmpty() || this.container.base.outputSlot[0].get(0).getCount() < 64) {
                        int coef = (int) (this.container.base.maxEnergySlots[0] / output1.getRecipe().output.metadata.getDouble("energy"));
                        this.drawText(poseStack,
                                input
                                        + output1.getRecipe().input.getInputs().get(0).getInputs().get(0).getCount() * coef
                                        + "x"
                                        + ModUtils.cleanComponentString(this.container.base.inputSlot[0].get(0).getDisplayName().getString()),
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 55,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        this.drawText(poseStack,
                                output
                                        + this.container.base.getOutput(0).getRecipe().output.items.get(0).getCount() * coef
                                        + "x"
                                        + ModUtils.cleanComponentString(output1.getRecipe().output.items.get(0).getDisplayName().getString()),
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 65,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        this.drawText(poseStack,
                                energyPerOperation + ModUtils.getString(this.container.base.energySlots[0]) + " EF",
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 75,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        if (this.container.base.getProgress(0) * 100.0 <= 100.0) {
                            this.drawText(poseStack,
                                    progress + (this.container.base.getProgress(0) <= 0.01 ? 0 : ModUtils.getString(this.container.base.getProgress(0) * 100.0)) + "%",
                                    iufix$guiLeft() + 60,
                                    iufix$guiTop() + 85,
                                    ModUtils.convertRGBcolorToInt(255, 255, 255)
                            );
                        }

                        this.drawText(poseStack,
                                "EF/t: " + ModUtils.getString(this.container.base.perenergy),
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 95,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        double hours = 0.0;
                        double minutes = 0.0;
                        double seconds = 0.0;
                        List<Double> time = this.container.base.getTime(0);
                        if (!time.isEmpty()) {
                            hours = time.get(0);
                            minutes = time.get(1);
                            seconds = time.get(2);
                        } else {
                            seconds = 1.0;
                        }

                        String time1 = hours > 0.0 ? ModUtils.getString(hours) + Localization.translate("iu.hour") : "";
                        String time2 = minutes > 0.0 ? ModUtils.getString(minutes) + Localization.translate("iu.minutes") : "";
                        String time3 = seconds > 0.0 ? ModUtils.getString(seconds) + Localization.translate("iu.seconds") : "";
                        this.drawText(poseStack,
                                Localization.translate("iu.timetoend") + time1 + time2 + time3,
                                iufix$guiLeft() + 60,
                                iufix$guiTop() + 105,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                    }
                }
            } else if (this.container.base.maxAmount == 2) {
                iufix$bindTexture();

                for (int i = 0; i < this.container.base.maxAmount; i++) {
                    chargeLevel = 20.0 * this.container.base.getProgress(i);
                    if (chargeLevel > 0.0
                            && !this.container.base.inputSlot[i].isEmpty()
                            && this.container.base.inputSlot[i].continue_proccess(this.container.base.outputSlot[i])) {
                        iufix$bindTexture();
                        iufix$drawTexturedModalRect(poseStack, iufix$guiLeft() + 26 + i * 20, iufix$guiTop() + 76, 44, 235, 14, (int) chargeLevel);
                        this.drawText(poseStack,
                                "EF/t: " + ModUtils.getString(this.container.base.perenergy),
                                iufix$guiLeft() + 85,
                                iufix$guiTop() + 55,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        double hours = 0.0;
                        double minutes = 0.0;
                        double seconds = 0.0;
                        List<Double> time = this.container.base.getTime(i);
                        if (!time.isEmpty()) {
                            hours = time.get(0);
                            minutes = time.get(1);
                            seconds = time.get(2);
                        } else {
                            seconds = 1.0;
                        }

                        String time1 = hours > 0.0 ? ModUtils.getString(hours) + Localization.translate("iu.hour") : "";
                        String time2 = minutes > 0.0 ? ModUtils.getString(minutes) + Localization.translate("iu.minutes") : "";
                        String time3 = seconds > 0.0 ? ModUtils.getString(seconds) + Localization.translate("iu.seconds") : "";
                        this.drawText(poseStack,
                                Localization.translate("iu.timetoend") + time1 + time2 + time3,
                                iufix$guiLeft() + 85,
                                iufix$guiTop() + 65 + i * 10,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                    }
                }
            } else if (this.container.base.maxAmount == 4) {
                iufix$bindTexture();

                for (int ix = 0; ix < this.container.base.maxAmount; ix++) {
                    chargeLevel = 20.0 * this.container.base.getProgress(ix);
                    if (chargeLevel > 0.0
                            && !this.container.base.inputSlot[ix].isEmpty()
                            && this.container.base.inputSlot[ix].continue_proccess(this.container.base.outputSlot[ix])) {
                        iufix$bindTexture();
                        iufix$drawTexturedModalRect(poseStack, iufix$guiLeft() + 10 + ix * 19, iufix$guiTop() + 76, 44, 235, 14, (int) chargeLevel);
                        this.drawText(poseStack,
                                "EF/t: " + ModUtils.getString(this.container.base.perenergy),
                                iufix$guiLeft() + 100,
                                iufix$guiTop() + 55,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                        double hours = 0.0;
                        double minutes = 0.0;
                        double seconds = 0.0;
                        List<Double> time = this.container.base.getTime(ix);
                        if (!time.isEmpty()) {
                            hours = time.get(0);
                            minutes = time.get(1);
                            seconds = time.get(2);
                        } else {
                            seconds = 1.0;
                        }

                        String time1 = hours > 0.0 ? ModUtils.getString(hours) + Localization.translate("iu.hour") : "";
                        String time2 = minutes > 0.0 ? ModUtils.getString(minutes) + Localization.translate("iu.minutes") : "";
                        String time3 = seconds > 0.0 ? ModUtils.getString(seconds) + Localization.translate("iu.seconds") : "";
                        this.drawText(poseStack,
                                Localization.translate("iu.timetoend") + time1 + time2 + time3,
                                iufix$guiLeft() + 100,
                                iufix$guiTop() + 65 + ix * 10,
                                ModUtils.convertRGBcolorToInt(255, 255, 255)
                        );
                    }
                }
            }
        } catch (Throwable t) {
            iufix$logRenderFailure(t, "m_7286_", this.container.base, 0);
        }
    }

    /** 等价于 ScreenIndustrialUpgrade.draw（protected 跨包不可直接调用），内联其实现。 */
    private void drawText(GuiGraphics poseStack, String s, int i, int i1, int i2) {
        poseStack.drawString(Minecraft.getInstance().font, s, i, i1, i2, false);
    }

    /**
     * drawForegroundLayer 竞态防线之一：GETFIELD output 重定向。
     * 若数组里出现 null（缓存被并发 setRecipeOutput(null) 清掉的窗口），
     * 返回 null 元素换成假配方的安全副本，随后的 output[i].getRecipe() 不再 NPE。
     */
    @Redirect(
            method = "drawForegroundLayer",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/denfop/blockentity/base/BlockEntityMolecularTransformer;output:[Lcom/denfop/api/recipe/MachineRecipe;"
            ),
            require = 1
    )
    private MachineRecipe[] iufix$safeOutputField(BlockEntityMolecularTransformer te) {
        MachineRecipe[] arr = te.output;
        boolean hasNull = false;
        for (MachineRecipe r : arr) {
            if (r == null) {
                hasNull = true;
                break;
            }
        }
        if (!hasNull) {
            return arr;
        }
        MachineRecipe[] safe = Arrays.copyOf(arr, arr.length);
        for (int j = 0; j < safe.length; j++) {
            if (safe[j] == null) {
                safe[j] = iufix$dummyRecipe();
            }
        }
        return safe;
    }

    /**
     * drawForegroundLayer 竞态防线之二：getOutput 调用重定向。
     * 实时命中→直通；null→缓存（命中打诊断）；缓存也 null→假配方兜底，绝不返回 null。
     */
    @Redirect(
            method = "drawForegroundLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/denfop/blockentity/base/BlockEntityMolecularTransformer;getOutput(I)Lcom/denfop/api/recipe/MachineRecipe;"
            ),
            require = 1
    )
    private MachineRecipe iufix$guardGetOutputForeground(BlockEntityMolecularTransformer te, int i) {
        MachineRecipe fresh = te.getOutput(i);
        if (fresh != null) {
            return fresh;
        }
        MachineRecipe cached = te.getRecipeOutput(i);
        iufix$logNullDiagnostics(te, i, "drawForegroundLayer", cached);
        return cached != null ? cached : iufix$dummyRecipe();
    }

    /** 兜底假配方（懒加载）：energy=1、空输入输出，仅供显示代码解引用，绝不参与匹配。 */
    private static MachineRecipe iufix$dummyRecipe() {
        MachineRecipe d = dummyRecipe;
        if (d == null) {
            synchronized (ScreenMolecularTransformerGuardMixin.class) {
                if (dummyRecipe == null) {
                    CompoundTag nbt = new CompoundTag();
                    nbt.putDouble("energy", 1.0);
                    IInputHandler factory = Recipes.inputFactory;
                    dummyRecipe = new MachineRecipe(
                            new BaseMachineRecipe(
                                    new Input(factory.getInput(new ItemStack(Items.AIR, 1))),
                                    new RecipeOutput(nbt, new ItemStack(Items.AIR, 1))
                            ),
                            Collections.singletonList(0)
                    );
                }
                d = dummyRecipe;
            }
        }
        return d;
    }

    private static void iufix$logRenderFailure(Throwable t, String site, BlockEntityMolecularTransformer te, int i) {
        String sig = site + '|' + t.getClass().getName();
        long now = System.nanoTime();
        if (sig.equals(lastLogSignature) && now - lastLogNanos < 5_000_000_000L) {
            return;
        }
        lastLogSignature = sig;
        lastLogNanos = now;
        StringBuilder sb = new StringBuilder();
        sb.append("[iufix] ScreenMolecularTransformer ").append(site).append(" 渲染异常已拦截（防崩）: ")
                .append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append('\n');
        sb.append("  渲染线程: ").append(Thread.currentThread().getName()).append('\n');
        StackTraceElement[] st = t.getStackTrace();
        for (int j = 0; j < st.length && j < 30; j++) {
            sb.append("    at ").append(st[j]).append('\n');
        }
        sb.append(iufix$dumpMachineState(te, i));
        LOGGER.warn(sb.toString());
    }

    private static void iufix$logNullDiagnostics(BlockEntityMolecularTransformer te, int i, String site, MachineRecipe cached) {
        String sig = i + "|null|" + iufix$recipeDesc(cached);
        long now = System.nanoTime();
        if (sig.equals(lastLogSignature) && now - lastLogNanos < 5_000_000_000L) {
            return;
        }
        lastLogSignature = sig;
        lastLogNanos = now;
        StringBuilder sb = new StringBuilder();
        sb.append("[iufix] ScreenMolecularTransformer getOutput(").append(i)
                .append(") 实时查表返回 null（site=").append(site)
                .append(cached != null ? "，回退缓存渲染防崩）" : "，缓存也为 null，回退假配方渲染防崩）").append('\n');
        sb.append(iufix$dumpMachineState(te, i));
        LOGGER.warn(sb.toString());
    }

    /** 竞态现场快照：输入槽内容 / 缓存配方 / 本槽快照 / 全局配方表。自带 try-catch，绝不再炸渲染。 */
    private static String iufix$dumpMachineState(BlockEntityMolecularTransformer te, int i) {
        StringBuilder sb = new StringBuilder();
        try {
            InventoryRecipes slot = te.inputSlot[i];
            ItemStack stack = slot.get(0);
            sb.append("  BE: queue=").append(te.queue)
                    .append(", maxAmount=").append(te.maxAmount)
                    .append(", 位置=").append(te.getBlockPos()).append('\n');
            sb.append("  当前线程: ").append(Thread.currentThread().getName()).append('\n');
            sb.append("  输入槽[").append(i).append("]: ").append(iufix$stackDesc(stack)).append('\n');
            sb.append("  缓存配方[").append(i).append("]: ").append(iufix$recipeDesc(te.getRecipeOutput(i))).append('\n');
            sb.append("  本槽 recipe_list 快照 (").append(slot.getRecipe_list() == null ? "null" : slot.getRecipe_list().size())
                    .append(" 条): ").append(iufix$recipeListDesc(slot.getRecipe_list())).append('\n');
            List<BaseMachineRecipe> global = Recipes.recipes.getRecipeList("molecular");
            sb.append("  全局 molecular 配方表 (").append(global == null ? "null" : global.size())
                    .append(" 条): ").append(iufix$recipeListDesc(global));
        } catch (Exception e) {
            sb.append("  （现场快照采集异常: ").append(e.getClass().getSimpleName()).append(':').append(e.getMessage()).append(')');
        }
        return sb.toString();
    }

    private static String iufix$stackDesc(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        sb.append(" x").append(stack.getCount());
        if (stack.getTag() != null) {
            String nbt = stack.getTag().toString();
            sb.append(" nbt=").append(iufix$truncate(nbt, 200));
        } else {
            sb.append(" nbt=null");
        }
        return sb.toString();
    }

    private static String iufix$recipeDesc(MachineRecipe recipe) {
        if (recipe == null || recipe.getRecipe() == null) {
            return "<null>";
        }
        BaseMachineRecipe base = recipe.getRecipe();
        StringBuilder sb = new StringBuilder();
        sb.append("input=[");
        List<IInputItemStack> inputs = base.input.getInputs();
        for (int j = 0; j < inputs.size(); j++) {
            if (j > 0) {
                sb.append(" | ");
            }
            IInputItemStack in = inputs.get(j);
            if (in.hasTag()) {
                sb.append("tag:").append(in.getTag().location());
            } else {
                List<ItemStack> stacks = in.getInputs();
                sb.append(stacks.isEmpty() ? "<?>" : iufix$stackDesc(stacks.get(0)));
            }
        }
        sb.append("] output=[");
        List<ItemStack> outs = base.output.items;
        for (int j = 0; j < outs.size(); j++) {
            if (j > 0) {
                sb.append(" | ");
            }
            sb.append(iufix$stackDesc(outs.get(j)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String iufix$recipeListDesc(List<BaseMachineRecipe> list) {
        if (list == null) {
            return "<null>";
        }
        if (list.isEmpty()) {
            return "<empty>";
        }
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(list.size(), 16);
        for (int j = 0; j < shown; j++) {
            if (j > 0) {
                sb.append(" || ");
            }
            BaseMachineRecipe base = list.get(j);
            List<IInputItemStack> inputs = base.input.getInputs();
            sb.append('[');
            for (int k = 0; k < inputs.size(); k++) {
                if (k > 0) {
                    sb.append(" | ");
                }
                IInputItemStack in = inputs.get(k);
                if (in.hasTag()) {
                    sb.append("tag:").append(in.getTag().location());
                } else {
                    List<ItemStack> stacks = in.getInputs();
                    sb.append(stacks.isEmpty() ? "<?>" : iufix$stackDesc(stacks.get(0)));
                }
            }
            sb.append(" -> ");
            List<ItemStack> outs = base.output.items;
            sb.append(outs.isEmpty() ? "<?>" : iufix$stackDesc(outs.get(0)));
            sb.append(']');
        }
        if (list.size() > shown) {
            sb.append(" ...(共 ").append(list.size()).append(" 条)");
        }
        return sb.toString();
    }

    private static String iufix$truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
