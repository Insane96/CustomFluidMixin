package insane96mcp.customfluidmixin.mixin;

import insane96mcp.customfluidmixin.data.CFM;
import insane96mcp.customfluidmixin.data.CFMListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidInteractionRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidInteractionRegistry.class)
public abstract class FluidInteractionRegistryMixin {

    @Inject(at = @At("HEAD"), method = "canInteract", cancellable = true, remap = false)
    private static void canInteract(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        for (CFM cfm : CFMListener.INSTANCE.getList()) {
            CFM.blockTransformation(cfm, level, level.getBlockState(pos), pos);
            if (CFM.fluidMixin(cfm, level, level.getBlockState(pos), pos)) {
                callback.setReturnValue(true);
                break;
            }
        }
    }
}