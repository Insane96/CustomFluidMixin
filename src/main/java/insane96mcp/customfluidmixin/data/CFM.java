package insane96mcp.customfluidmixin.data;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import insane96mcp.insanelib.data.IdTagMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

@JsonAdapter(CFM.Serializer.class)
public class CFM {
    public boolean jeiOnly;
    public Type type;
    public MixinResult result;
    public boolean fizz;
    public IdTagMatcher flowing;
    public IdTagMatcher blockToTransform;
    public List<IdTagMatcher> blocksNearby;

    public static class Serializer implements JsonDeserializer<CFM>, JsonSerializer<CFM> {
        @Override
        public CFM deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            CFM cfm = new CFM();
            JsonObject jObject = json.getAsJsonObject();
            cfm.jeiOnly = GsonHelper.getAsBoolean(jObject, "jei_only", false);
            cfm.type = context.deserialize(jObject.get("type"), Type.class);
            cfm.flowing = context.deserialize(jObject.get("flowing"), IdTagMatcher.class);
            if (cfm.type == Type.BLOCK_TRANSFORM) {
                cfm.blockToTransform = context.deserialize(jObject.get("block_to_transform"), IdTagMatcher.class);
            }
            cfm.blocksNearby = context.deserialize(jObject.get("blocks_nearby"), IdTagMatcher.LIST_TYPE);
            if (cfm.type == Type.FLOWING_MIXIN) {
                if (cfm.blocksNearby.isEmpty())
                    throw new JsonParseException("Invalid blocks_nearby. There must be at least one block nearby when using \"flowing_block\"");
                else if (cfm.blocksNearby.size() > 5)
                    throw new JsonParseException("Invalid blocks_nearby. There must at most 5 blocks");
            }

            cfm.result = context.deserialize(jObject.get("result"), MixinResult.class);
            cfm.fizz = GsonHelper.getAsBoolean(jObject, "fizz", true);

            return cfm;
        }

        @Override
        public JsonElement serialize(CFM cfm, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jObject = new JsonObject();
            if (cfm.jeiOnly)
                jObject.addProperty("jei_only", true);
            jObject.add("type", context.serialize(cfm.type));
            jObject.add("flowing", context.serialize(cfm.flowing));
            if (cfm.type == Type.BLOCK_TRANSFORM)
                jObject.add("block_to_transform", context.serialize(cfm.blockToTransform));
            if (cfm.blocksNearby != null)
                jObject.add("blocks_nearby", context.serialize(cfm.blocksNearby));
            jObject.add("result", context.serialize(cfm.result));
            if (!cfm.fizz)
                jObject.addProperty("fizz", false);

            return jObject;
        }
    }

    public static CFM createFlowingMixin(String flowing, List<IdTagMatcher> blocksNearby, String blockResult) {
        CFM cfm = new CFM();
        cfm.type = Type.FLOWING_MIXIN;
        cfm.flowing = new IdTagMatcher(IdTagMatcher.Type.ID, new ResourceLocation(flowing));
        cfm.blocksNearby = blocksNearby;
        cfm.result = MixinResult.newBlockResult(blockResult);
        return cfm;
    }

    public static CFM createBlockTransformation(String flowing, IdTagMatcher blockToTransform, List<IdTagMatcher> blocksNearby, String blockResult) {
        CFM cfm = new CFM();
        cfm.type = Type.BLOCK_TRANSFORM;
        cfm.flowing = new IdTagMatcher(IdTagMatcher.Type.ID, new ResourceLocation(flowing));
        cfm.blockToTransform = blockToTransform;
        cfm.blocksNearby = blocksNearby;
        cfm.result = MixinResult.newBlockResult(blockResult);
        return cfm;
    }

    /**
     * Returns a list with all the fluid stacks in the "flowing" key
     */
    public List<FluidStack> getFlowingStacks() {
        return this.flowing.getAllFluidStacks();
    }

    /**
     * Returns a list with all the fluids stacks in the "block_to_transform" key
     */
    public List<FluidStack> getFluidToTransformStacks() {
        return this.blockToTransform.getAllFluidStacks();
    }

    /**
     * Returns a list with all the block stacks in the "block_to_transform" key
     */
    public List<ItemStack> getBlockToTransformStacks() {
        return this.blockToTransform.getAllItemStacks();
    }

    @Override
    public String toString() {
        return String.format("CFM[type: %s, flowing: %s, block_to_transform: %s, blocks_nearby: %s, result: %s, fizz: %s, jei_only: %s]", this.type, this.flowing, this.blockToTransform, this.blocksNearby, this.result, this.fizz, this.jeiOnly);
    }

    public enum Type {
        @SerializedName("flowing_block")
        FLOWING_MIXIN,
        @SerializedName("block_transform")
        BLOCK_TRANSFORM
    }

    /**
     * Returns true if a mixin has been successful and shouldn't keep executing vanilla code
     */
    public static boolean fluidMixin(CFM cfm, Level level, BlockState state, BlockPos pos) {
        if (cfm.jeiOnly
                || cfm.type != Type.FLOWING_MIXIN
                || !cfm.flowing.matchesFluid(state.getFluidState().getType()))
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
            level.levelEvent(1501, pos, 0);
        return true;
    }

    /**
     * Returns true if a mixin has been successful and shouldn't keep executing vanilla code
     */
    public static void blockTransformation(CFM cfm, Level level, BlockState state, BlockPos pos) {
        if (cfm.jeiOnly
                || cfm.type != Type.BLOCK_TRANSFORM
                || !cfm.flowing.matchesFluid(state.getFluidState().getType()))
            return;

        //For each flowing direction (everywhere but up)
        for (Direction fluidDirection : LiquidBlock.POSSIBLE_FLOW_DIRECTIONS) {
            BlockPos posFluidDirection = pos.relative(fluidDirection);
            BlockState newState = cfm.result.getRandomBlockResult(level.random).getState();
            //If the fluid doesn't match
            if ((level.getFluidState(posFluidDirection).getType() != Fluids.EMPTY && !cfm.blockToTransform.matchesFluid(level.getFluidState(posFluidDirection).getType()))
                    // Or the block to transform doesn't match
                    || (cfm.type == Type.BLOCK_TRANSFORM && (!cfm.blockToTransform.matchesBlock(level.getBlockState(posFluidDirection).getBlock())
                    // Or the current block is already the block to transform to
                    || newState.is(level.getBlockState(posFluidDirection).getBlock()))))
                //Do nothing
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
                level.levelEvent(1501, pos, 0);
        }
    }
}
