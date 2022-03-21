package insane96mcp.customfluidmixin.data;

import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import insane96mcp.customfluidmixin.CustomFluidMixin;
import insane96mcp.customfluidmixin.exception.JsonValidationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class CFMListener extends SimpleJsonResourceReloadListener {

	public static final CFMListener INSTANCE;
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public final Map<ResourceLocation, CFM> customFluidMixin;

    public CFMListener() {
        super(GSON, "custom_fluid_mixin");
        this.customFluidMixin = Maps.newHashMap();
    }
    
    static {
        INSTANCE = new CFMListener();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
		CustomFluidMixin.LOGGER.info("Reloading Custom Fluid Mixin");
        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
			ResourceLocation name = entry.getKey();
			String[] split = name.getPath().split("/");
			if (split[split.length - 1].startsWith("_"))
				continue;
			JsonElement json = entry.getValue();
			CFM cfm = null;
			try {
                cfm = GSON.fromJson(json, CFM.class);
                cfm.validate();

                this.customFluidMixin.put(name, cfm);
			}
            catch (JsonValidationException e) {
                CustomFluidMixin.LOGGER.error("Validation error loading Custom Fluid Mixin {}: {}", entry.getKey(), e.getMessage());
            }
			catch (JsonSyntaxException e) {
                CustomFluidMixin.LOGGER.error("Parsing error loading Custom Fluid Mixin {}: {}", entry.getKey(), e.getMessage());
			}
		}

		CustomFluidMixin.LOGGER.info("{} Custom Fluid Mixins loaded!", this.customFluidMixin.size());
    }
}
