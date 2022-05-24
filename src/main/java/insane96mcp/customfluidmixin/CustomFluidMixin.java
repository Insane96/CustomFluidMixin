package insane96mcp.customfluidmixin;

import insane96mcp.customfluidmixin.data.CFMListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CustomFluidMixin.MOD_ID)
public class CustomFluidMixin
{
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "customfluidmixin";

    public CustomFluidMixin() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerAboutToStart(AddReloadListenerEvent event) {
        event.addListener(CFMListener.INSTANCE);
    }
}
