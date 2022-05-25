package insane96mcp.customfluidmixin.integration.jei;

import insane96mcp.customfluidmixin.CustomFluidMixin;
import insane96mcp.customfluidmixin.data.CFM;
import insane96mcp.customfluidmixin.data.CFMListener;
import insane96mcp.insanelib.util.IdTagMatcher;
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

    public static final RecipeType<CFM> CFM =
            RecipeType.create(CFMCategory.CATEGORY_ID.getNamespace(), CFMCategory.CATEGORY_ID.getPath(), CFM.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CFMCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    private final CFM CFM_COBBLESTONE = new CFM("minecraft:lava", List.of(
            new IdTagMatcher(null, new ResourceLocation("minecraft:water"))),
            "minecraft:cobblestone");
    private final CFM CFM_BASALT = new CFM("minecraft:lava", List.of(
            new IdTagMatcher(new ResourceLocation("minecraft:soul_soil"), null),
            new IdTagMatcher(new ResourceLocation("minecraft:blue_ice"), null)),
            "minecraft:basalt");

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ArrayList<CFM> recipes = new ArrayList<>(CFMListener.INSTANCE.getList());
        recipes.add(CFM_COBBLESTONE);
        recipes.add(CFM_BASALT);
        registration.addRecipes(CFM, recipes);
    }
}
