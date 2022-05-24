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
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
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
            int x = 48 + (catalysts * 16) - (catalysts / 3 * 48);
            int y = 11 + (catalysts / 3 * 16);
            if (blocksNearby.size() <= 3)
                y += 6;
            builder.addSlot(RecipeIngredientRole.CATALYST, x, y)
                    .addItemStacks(blockNearby);
            catalysts++;
        }
    }

    @Override
    public void draw(CFM recipe, IRecipeSlotsView recipeSlotsView, PoseStack poseStack, double mouseX, double mouseY) {
        if (recipe.result.type == CFM.MixinResult.Type.EXPLOSION) {
            Minecraft minecraft = Minecraft.getInstance();
            drawRepairCost(minecraft, poseStack, I18n.get("jei.result.explosion"), 0xFF32A852);
        }
        else if (recipe.result.type == CFM.MixinResult.Type.FUNCTION) {
            Minecraft minecraft = Minecraft.getInstance();
            drawRepairCost(minecraft, poseStack, I18n.get("jei.result.function"), 0xFFA7C425);
        }
    }

    private void drawRepairCost(Minecraft minecraft, PoseStack poseStack, String text, int mainColor) {
        int shadowColor = 0xFF000000 | (mainColor & 0xFCFCFC) >> 2;
        int width = minecraft.font.width(text);
        int x = background.getWidth() - 15 - width / 2;
        int y = 22;

        // TODO 1.13 match the new GuiRepair style
        minecraft.font.draw(poseStack, text, x + 1, y, shadowColor);
        minecraft.font.draw(poseStack, text, x, y + 1, shadowColor);
        minecraft.font.draw(poseStack, text, x + 1, y + 1, shadowColor);
        minecraft.font.draw(poseStack, text, x, y, mainColor);
    }

    @Override
    public List<Component> getTooltipStrings(CFM recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (recipe.result.type == CFM.MixinResult.Type.EXPLOSION) {
            if (mouseX >= 137 && mouseX <= 161 && mouseY >= 22 && mouseY <= 30)
                return Arrays.asList(new TranslatableComponent("jei.result.explosion.tooltip", recipe.result.explosionPower));
        }
        else if (recipe.result.type == CFM.MixinResult.Type.FUNCTION) {
            if (mouseX >= 140 && mouseX <= 159 && mouseY >= 22 && mouseY <= 29)
                return Arrays.asList(new TranslatableComponent("jei.result.function.tooltip", recipe.result.function.getId()));
        }
        return IRecipeCategory.super.getTooltipStrings(recipe, recipeSlotsView, mouseX, mouseY);
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
