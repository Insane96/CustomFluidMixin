package insane96mcp.customfluidmixin.integration.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import insane96mcp.customfluidmixin.CustomFluidMixin;
import insane96mcp.customfluidmixin.data.CFM;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.GuiUtils;

import java.util.ArrayList;
import java.util.List;

public class CFMCategory implements IRecipeCategory<CFM> {

    public static final ResourceLocation CATEGORY_ID = new ResourceLocation(CustomFluidMixin.MOD_ID, "fluid_mixin");
    public static final int width = 164;
    public static final int height = 50;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component localizedName;

    public CFMCategory(IGuiHelper guiHelper) {
        ResourceLocation location = Constants.JEI_GUI;
        background = guiHelper.createDrawable(location, 0, 0, width, height);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.LAVA_BUCKET));
        localizedName = new TranslatableComponent("gui.jei.category.cfm");
    }

    @Override
    public RecipeType<CFM> getRecipeType() {
        return new RecipeType<>(CATEGORY_ID, CFM.class);
    }

    @Override
    public Component getTitle() {
        return this.localizedName;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CFM recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 7, 17)
                .addItemStacks(recipe.getFlowingStacks());

        if (recipe.result.type == CFM.MixinResult.Type.BLOCK)
            builder.addSlot(RecipeIngredientRole.OUTPUT, 141, 18)
                    .addItemStack(new ItemStack(recipe.result.block.getBlock()));

        List<List<ItemStack>> blocksNearby = recipe.getBlocksNearby();
        int catalysts = 0;
        for (List<ItemStack> blockNearby : blocksNearby) {
            int x = 51 + (catalysts * 18) - (catalysts / 3 * 54);
            int y = 8 + (catalysts / 3 * 18);
            if (blocksNearby.size() <= 3)
                y += 8;
            builder.addSlot(RecipeIngredientRole.CATALYST, x, y)
                    .addItemStacks(blockNearby);
            catalysts++;
        }
    }

    @Override
    public void draw(CFM recipe, IRecipeSlotsView recipeSlotsView, PoseStack poseStack, double mouseX, double mouseY) {
        if (recipe.result.type != CFM.MixinResult.Type.BLOCK) {
            drawRepairCost(poseStack, recipe.result.type);
        }
    }

    private void drawRepairCost(PoseStack poseStack, CFM.MixinResult.Type type) {
        int x = 141;
        int y = 18;
        int u = type.equals(CFM.MixinResult.Type.EXPLOSION) ? 0 : 16;
        int v = 50;
        GuiUtils.drawTexturedModalRect(poseStack, x, y, u, v, 16, 16, 0f);
        /*int shadowColor = 0xFF000000 | (mainColor & 0xFCFCFC) >> 2;
        int width = minecraft.font.width(text);

        // TODO 1.13 match the new GuiRepair style
        minecraft.font.draw(poseStack, text, x + 1, y, shadowColor);
        minecraft.font.draw(poseStack, text, x, y + 1, shadowColor);
        minecraft.font.draw(poseStack, text, x + 1, y + 1, shadowColor);
        minecraft.font.draw(poseStack, text, x, y, mainColor);*/
    }

    @Override
    public List<Component> getTooltipStrings(CFM recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        ArrayList<Component> tooltips = new ArrayList<>();
        if (mouseX >= 141 && mouseX <= 157 && mouseY >= 18 && mouseY <= 34) {
            if (recipe.result.type == CFM.MixinResult.Type.EXPLOSION) {
                tooltips.add(new TranslatableComponent("jei.result.explosion.tooltip", recipe.result.explosionPower));
            }
            else if (recipe.result.type == CFM.MixinResult.Type.FUNCTION) {
                tooltips.add(new TranslatableComponent("jei.result.function.tooltip", recipe.result.function.getId()));
            }
        }
        if (mouseX >= 29 && mouseX <= 42 && mouseY >= 21 && mouseY <= 28)
            tooltips.add(new TranslatableComponent("jei.flowing_into"));
        return tooltips;
    }

    @SuppressWarnings("removal")
    @Override
    public ResourceLocation getUid() {
        return CATEGORY_ID;
    }

    @SuppressWarnings("removal")
    @Override
    public Class<? extends CFM> getRecipeClass() {
        return CFM.class;
    }
}
