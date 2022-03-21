package insane96mcp.customfluidmixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import insane96mcp.customfluidmixin.data.CFMListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod("customfluidmixin")
public class CustomFluidMixin
{
    public static final Logger LOGGER = LogManager.getLogger();

    public CustomFluidMixin() {
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, CFMConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerAboutToStart(AddReloadListenerEvent event) {
        event.addListener(CFMListener.INSTANCE);
    }
}
