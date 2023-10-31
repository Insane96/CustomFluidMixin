package insane96mcp.customfluidmixin.integration.jei;

import insane96mcp.customfluidmixin.CustomFluidMixin;
import insane96mcp.customfluidmixin.data.CFM;
import insane96mcp.customfluidmixin.data.CFMListener;
import insane96mcp.insanelib.data.IdTagMatcher;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class CFMJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID = new ResourceLocation(CustomFluidMixin.MOD_ID, "jei_plugin");

    public static final RecipeType<CFM> CFM_FLUID_MIXIN =
            RecipeType.create(CFMFluidMixinCategory.CATEGORY_ID.getNamespace(), CFMFluidMixinCategory.CATEGORY_ID.getPath(), CFM.class);

    public static final RecipeType<CFM> CFM_BLOCK_TRANSFORM =
            RecipeType.create(CFMBlockTransformationCategory.CATEGORY_ID.getNamespace(), CFMBlockTransformationCategory.CATEGORY_ID.getPath(), CFM.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CFMFluidMixinCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CFMBlockTransformationCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    private final CFM CFM_COBBLESTONE = CFM.createFlowingMixin("minecraft:lava",
            List.of(
                    new IdTagMatcher(IdTagMatcher.Type.ID, new ResourceLocation("minecraft:water"))),
            "minecraft:cobblestone");
    private final CFM CFM_BASALT = CFM.createFlowingMixin("minecraft:lava",
            List.of(
                    new IdTagMatcher(IdTagMatcher.Type.ID, new ResourceLocation("minecraft:soul_soil")),
                    new IdTagMatcher(IdTagMatcher.Type.ID, new ResourceLocation("minecraft:blue_ice"))),
            "minecraft:basalt");
    private final CFM CFM_OBSIDIAN = CFM.createBlockTransformation("minecraft:water",
            new IdTagMatcher(IdTagMatcher.Type.ID, new ResourceLocation("minecraft:lava")),
            List.of(),
            "minecraft:obsidian");
    private final CFM CFM_STONE = CFM.createBlockTransformation("minecraft:lava",
            new IdTagMatcher(IdTagMatcher.Type.ID, new ResourceLocation("minecraft:water")),
            List.of(),
            "minecraft:stone");

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ArrayList<CFM> fluidMixin = new ArrayList<>(CFMListener.INSTANCE.getFluidMixinList());
        fluidMixin.add(CFM_COBBLESTONE);
        fluidMixin.add(CFM_BASALT);
        registration.addRecipes(CFM_FLUID_MIXIN, fluidMixin);

        ArrayList<CFM> blockTransformation = new ArrayList<>(CFMListener.INSTANCE.getBlockTransformationList());
        blockTransformation.add(CFM_OBSIDIAN);
        blockTransformation.add(CFM_STONE);
        registration.addRecipes(CFM_BLOCK_TRANSFORM, blockTransformation);
    }
}
