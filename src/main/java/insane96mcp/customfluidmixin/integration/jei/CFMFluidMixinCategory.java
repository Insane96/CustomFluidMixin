package insane96mcp.customfluidmixin.integration.jei;

import insane96mcp.customfluidmixin.CustomFluidMixin;
import insane96mcp.customfluidmixin.data.CFM;
import insane96mcp.insanelib.util.IdTagMatcher;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CFMFluidMixinCategory implements IRecipeCategory<CFM> {

    public static final ResourceLocation CATEGORY_ID = new ResourceLocation(CustomFluidMixin.MOD_ID, "fluid_mixin");
    public static final int width = 164;
    public static final int height = 50;

    private final IDrawable background;
    private final IDrawable icon;
    private final Component localizedName;

    public CFMFluidMixinCategory(IGuiHelper guiHelper) {
        ResourceLocation location = Constants.JEI_GUI;
        background = guiHelper.createDrawable(location, 0, 0, width, height);
        icon = guiHelper.createDrawable(Constants.JEI_GUI, 32, 100, 16, 16);
        localizedName = Component.translatable("jei.category.fluid_mixin");
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
                .addIngredients(ForgeTypes.FLUID_STACK, recipe.getFlowingStacks());

        if (recipe.result.type == CFM.MixinResult.Type.BLOCK)
            builder.addSlot(RecipeIngredientRole.OUTPUT, 141, 18)
                    .addItemStack(new ItemStack(recipe.result.block.getBlock()));

        List<IdTagMatcher> blocksNearby = recipe.blocksNearby;
        int catalysts = 0;
        for (IdTagMatcher blockNearby : blocksNearby) {
            int x = 52 + (catalysts * 17) - (catalysts / 3 * 51);
            int y = 8 + (catalysts / 3 * 18);
            if (blocksNearby.size() <= 3)
                y += 8;
            builder.addSlot(RecipeIngredientRole.CATALYST, x, y)
                        .addIngredients(ForgeTypes.FLUID_STACK, blockNearby.getAllFluidStacks())
                        .addItemStacks(blockNearby.getAllItemStacks());
            catalysts++;
        }
    }

    @Override
    public void draw(CFM recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        if (recipe.result.type != CFM.MixinResult.Type.BLOCK) {
            drawNonBlockResult(guiGraphics, recipe.result.type);
        }
    }

    private void drawNonBlockResult(GuiGraphics guiGraphics, CFM.MixinResult.Type type) {
        int x = 141;
        int y = 18;
        int u = Constants.ICONS_U + (type.equals(CFM.MixinResult.Type.EXPLOSION) ? 0 : 16);
        int v = Constants.ICONS_V;
        guiGraphics.blit(Constants.JEI_GUI, x, y, u, v, 16, 16);
    }

    @Override
    public List<Component> getTooltipStrings(CFM recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        ArrayList<Component> tooltips = new ArrayList<>();
        if (mouseX >= 141 && mouseX <= 157 && mouseY >= 18 && mouseY <= 34) {
            if (recipe.result.type == CFM.MixinResult.Type.EXPLOSION) {
                tooltips.add(Component.translatable("jei.result.explosion.tooltip", recipe.result.explosionPower));
            }
            else if (recipe.result.type == CFM.MixinResult.Type.FUNCTION) {
                tooltips.add(Component.translatable("jei.result.function.tooltip", recipe.result.function.getId()));
            }
        }
        if (mouseX >= 29 && mouseX <= 42 && mouseY >= 21 && mouseY <= 28)
            tooltips.add(Component.translatable("jei.flowing_into"));
        return tooltips;
    }
}
