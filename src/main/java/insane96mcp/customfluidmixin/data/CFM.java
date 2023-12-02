package insane96mcp.customfluidmixin.data;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import insane96mcp.insanelib.data.IdTagMatcher;
import net.minecraft.commands.CommandFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@JsonAdapter(CFM.Serializer.class)
public class CFM {
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
            cfm.type = context.deserialize(jObject.get("type"), Type.class);
            cfm.flowing = context.deserialize(jObject.get("flowing"), IdTagMatcher.class);
            if (cfm.type == Type.BLOCK_TRANSFORM) {
                cfm.blockToTransform = context.deserialize(jObject.get("block_to_transform"), IdTagMatcher.class);
            }
            cfm.blocksNearby = context.deserialize(jObject.get("blocks_nearby"), IdTagMatcher.LIST_TYPE);
            if (cfm.type == Type.FLOWING_MIXIN && cfm.blocksNearby.isEmpty())
                throw new JsonParseException("Invalid blocks_nearby. There must be at least one block nearby when using \"flowing_block\"");
            else if (cfm.blocksNearby.size() > 5)
                throw new JsonParseException("Invalid blocks_nearby. There must at most 5 blocks");

            cfm.result = context.deserialize(jObject.get("result"), MixinResult.class);
            cfm.fizz = GsonHelper.getAsBoolean(jObject, "fizz", true);

            return cfm;
        }

        @Override
        public JsonElement serialize(CFM cfm, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jObject = new JsonObject();
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
        return String.format("CFM[type: %s, flowing: %s, block_to_transform: %s, blocks_nearby: %s, result: %s, fizz: %s]", this.type, this.flowing, this.blockToTransform, this.blocksNearby, this.result, this.fizz);
    }

    @JsonAdapter(MixinResult.Serializer.class)
    public static class MixinResult {
        public Type type;
        public float explosionPower;
        public Boolean shouldGenerateFire;
        public float chance;
        public BlockState block;
        public CommandFunction.CacheableFunction function;

        public static class Serializer implements JsonDeserializer<MixinResult>, JsonSerializer<MixinResult> {
            @Override
            public MixinResult deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                MixinResult mixinResult = new MixinResult();
                JsonObject jObject = json.getAsJsonObject();
                mixinResult.type = context.deserialize(jObject.get("type"), MixinResult.Type.class);
                switch (mixinResult.type) {
                    case BLOCK -> mixinResult.block = ForgeRegistries.BLOCKS.getValue(context.deserialize(jObject.get("block"), ResourceLocation.class)).defaultBlockState();
                    case EXPLOSION -> {
                        mixinResult.explosionPower = GsonHelper.getAsFloat(jObject, "explosion_power");
                        mixinResult.shouldGenerateFire = GsonHelper.getAsBoolean(jObject, "fire", false);
                    }
                    case FUNCTION -> mixinResult.function = new CommandFunction.CacheableFunction(new ResourceLocation(GsonHelper.getAsString(jObject, "function")));
                }
                mixinResult.chance = GsonHelper.getAsFloat(jObject, "chance", 1f);
                return mixinResult;
            }

            @Override
            public JsonElement serialize(MixinResult mixinResult, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jObject = new JsonObject();
                jObject.add("type", context.serialize(mixinResult.type));
                switch (mixinResult.type) {
                    case BLOCK -> jObject.add("block", context.serialize(mixinResult.block));
                    case EXPLOSION -> {
                        jObject.addProperty("explosion_power", mixinResult.explosionPower);
                        if (mixinResult.shouldGenerateFire)
                            jObject.addProperty("fire", true);
                    }
                    case FUNCTION ->
                            jObject.add("function", context.serialize(mixinResult.function.getId()));
                }
                jObject.addProperty("chance", mixinResult.chance);

                return jObject;
            }
        }

        public static MixinResult newBlockResult(String block) {
            MixinResult m = new MixinResult();
            m.type = Type.BLOCK;
            m.block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(block)).defaultBlockState();
            return m;
        }

        public void execute(ServerLevel level, BlockPos pos) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            if (level.getRandom().nextFloat() > this.chance)
                return;

            switch (this.type) {
                case BLOCK -> level.setBlockAndUpdate(pos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, block));
                case EXPLOSION -> level.explode(null, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, explosionPower, this.shouldGenerateFire, Level.ExplosionInteraction.BLOCK);
                case FUNCTION -> {
                    MinecraftServer server = level.getServer();
                    this.function.get(server.getFunctions()).ifPresent((commandFunction) -> server.getFunctions().execute(commandFunction, server.getFunctions().getGameLoopSender().withPosition(new Vec3(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d)).withLevel(level)));
                }
            }
        }

        public enum Type {
            @SerializedName("block")
            BLOCK,
            @SerializedName("explosion")
            EXPLOSION,
            @SerializedName("function")
            FUNCTION
        }
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
            level.levelEvent(1501, pos, 0);
        return true;
    }

    /**
     * Returns true if a mixin has been successful and shouldn't keep executing vanilla code
     */
    public static void blockTransformation(CFM cfm, Level level, BlockState state, BlockPos pos) {
        if (cfm.type != CFM.Type.BLOCK_TRANSFORM)
            return;
        if (!cfm.flowing.matchesFluid(state.getFluidState().getType()))
            return;

        //For each flowing direction (everywhere but up)
        for (Direction fluidDirection : LiquidBlock.POSSIBLE_FLOW_DIRECTIONS) {
            BlockPos posFluidDirection = pos.relative(fluidDirection);
            //If the fluid doesn't match
            if ((level.getFluidState(posFluidDirection).getType() != Fluids.EMPTY && !cfm.blockToTransform.matchesFluid(level.getFluidState(posFluidDirection).getType()))
                    // Or the block to transform doesn't match
                    || (cfm.type == Type.BLOCK_TRANSFORM && (!cfm.blockToTransform.matchesBlock(level.getBlockState(posFluidDirection).getBlock())
                            // Or the current block is already the block to transform to
                            || cfm.result.block.is(level.getBlockState(posFluidDirection).getBlock()))))
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
