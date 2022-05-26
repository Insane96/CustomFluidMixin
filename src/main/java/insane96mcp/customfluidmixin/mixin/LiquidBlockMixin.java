package insane96mcp.customfluidmixin.mixin;

import insane96mcp.customfluidmixin.data.CFM;
import insane96mcp.customfluidmixin.data.CFMListener;
import insane96mcp.insanelib.util.IdTagMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
    private void shouldSpreadLiquid(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> callback) {
        for (CFM cfm : CFMListener.INSTANCE.getList()) {
            blockTransformation(cfm, level, state, pos);
            if (fluidMixin(cfm, level, state, pos)) {
                callback.setReturnValue(false);
                break;
            }
        }
    }

    /**
     * Returns true if a mixin has been successful and shouldn't keep executing vanilla code
     */
    private boolean fluidMixin(CFM cfm, Level level, BlockState state, BlockPos pos) {
        if (cfm.type != CFM.Type.FLOWING_MIXIN)
            return false;
        if (!cfm.flowing.matchesFluid(state.getFluidState().getType()))
            return false;

        boolean blocksNearbyMatch = true;
        for (IdTagMatcher blockAround : cfm.blocksNearby) {
            boolean found = false;
            for (Direction direction : Direction.values()) {
                BlockPos blockpos = pos.relative(direction);
                if (blockAround.matchesBlock(level.getBlockState(blockpos).getBlock()) || blockAround.matchesFluid(level.getFluidState(blockpos).getType())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                blocksNearbyMatch = false;
                break;
            }
        }
        if (!blocksNearbyMatch)
            return false;

        cfm.result.execute((ServerLevel) level, pos);
        if (cfm.fizz)
            this.fizz(level, pos);
        return true;
    }

    /**
     * Returns true if a mixin has been successful and shouldn't keep executing vanilla code
     */
    private void blockTransformation(CFM cfm, Level level, BlockState state, BlockPos pos) {
        if (cfm.type != CFM.Type.BLOCK_TRANSFORM)
            return;
        if (!cfm.flowing.matchesFluid(state.getFluidState().getType()))
            return;

        for (Direction fluidDirection : LiquidBlock.POSSIBLE_FLOW_DIRECTIONS) {
            BlockPos posFluidDirection = pos.relative(fluidDirection);
            if (!cfm.blockToTransform.matchesBlock(level.getBlockState(posFluidDirection).getBlock()) && !cfm.blockToTransform.matchesFluid(level.getFluidState(posFluidDirection).getType()))
                continue;
            boolean blocksNearbyMatch = true;
            for (IdTagMatcher blockAround : cfm.blocksNearby) {
                boolean found = false;
                for (Direction direction : Direction.values()) {
                    BlockPos blockPos = posFluidDirection.relative(direction);
                    if (blockAround.matchesBlock(level.getBlockState(blockPos).getBlock()) || blockAround.matchesFluid(level.getFluidState(blockPos).getType())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    blocksNearbyMatch = false;
                    break;
                }
            }
            if (!blocksNearbyMatch)
                continue;

            cfm.result.execute((ServerLevel) level, posFluidDirection);
            if (cfm.fizz)
                this.fizz(level, pos);
        }
    }

    @Shadow
    protected abstract void fizz(LevelAccessor worldIn, BlockPos pos);
}