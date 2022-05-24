package insane96mcp.customfluidmixin.integration.jei;

import insane96mcp.customfluidmixin.CustomFluidMixin;
import insane96mcp.customfluidmixin.data.CFM;
import insane96mcp.customfluidmixin.data.CFMListener;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

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

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CFM, CFMListener.INSTANCE.getList().stream().toList());
    }
}
