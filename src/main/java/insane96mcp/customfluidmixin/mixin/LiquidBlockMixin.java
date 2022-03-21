package insane96mcp.customfluidmixin.mixin;

import insane96mcp.customfluidmixin.CustomFluidMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {

    @Inject(at = @At("HEAD"), method = "shouldSpreadLiquid(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", cancellable = true)
    private void reactWithNeighbors(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> callback) {
        if (CustomFluidMixin.customFluidMix(level, pos, state)) {
            this.fizz(level, pos);
            callback.setReturnValue(false);
        }
    }

    @Shadow
    protected abstract void fizz(LevelAccessor worldIn, BlockPos pos);
}