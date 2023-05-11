package insane96mcp.customfluidmixin.data;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import insane96mcp.insanelib.util.IdTagMatcher;
import net.minecraft.commands.CommandFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class CFM {

    @SerializedName("type")
    public Type type;

    @SerializedName("flowing")
    private String _flowing;

    @SerializedName("block_to_transform")
    private String _blockToTransform;

    @SerializedName("blocks_nearby")
    private List<String> _blocksNearby;

    public MixinResult result;

    public Boolean fizz;

    public transient IdTagMatcher flowing;
    public transient IdTagMatcher blockToTransform;
    public transient List<IdTagMatcher> blocksNearby;

    public void validate() throws JsonParseException {
        if (type == null)
            throw new JsonParseException("Missing type");

        if (_flowing == null)
            throw new JsonParseException("Missing flowing block");
        flowing = IdTagMatcher.parseLine(_flowing);
        if (flowing == null)
            throw new JsonParseException("Failed to parse flowing");

        if (type == Type.BLOCK_TRANSFORM) {
            if (_blockToTransform == null)
                throw new JsonParseException("Missing block_to_transform block");
            blockToTransform = IdTagMatcher.parseLine(_blockToTransform);
            if (blockToTransform == null)
                throw new JsonParseException("Failed to parse block_to_transform");
        }

        if (_blocksNearby == null)
            throw new JsonParseException("Missing blocks_nearby");
        int minBlocksNearby = this.type == Type.FLOWING_MIXIN ? 1 : 0;
        if (_blocksNearby.size() < minBlocksNearby || _blocksNearby.size() > 5)
            throw new JsonParseException("Invalid blocks_nearby. There must be at least one block nearby (0 works too if type = 'block_transform') and less than 5 blocks");

        blocksNearby = new ArrayList<>();
        for (String s : _blocksNearby) {
            IdTagMatcher idTagMatcher = IdTagMatcher.parseLine(s);
            if (idTagMatcher == null)
                throw new JsonParseException("Failed to parse a block nearby " + s);
            blocksNearby.add(idTagMatcher);
        }

        if (result == null)
            throw new JsonParseException("Missing result");
        result.validate();

        if (this.fizz == null)
            this.fizz = true;
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
        return String.format("CFM[flowing: %s, blocks_nearby: %s, result: %s]", this.flowing, this.blocksNearby,
                this.result);
    }

    public static class MixinResult {
        public Type type;
        @SerializedName("block")
        private String _block;
        @SerializedName("power")
        public Float explosionPower;
        @SerializedName("fire")
        public Boolean shouldGenerateFire;
        @SerializedName("nbt")
        private String _nbt;
        @SerializedName("function")
        private String _function;
        public Float chance;

        public transient BlockState block;
        public transient CommandFunction.CacheableFunction function;

        public static MixinResult newBlockResult(String block) {
            MixinResult m = new MixinResult();
            m.type = Type.BLOCK;
            m.block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(block)).defaultBlockState();
            return m;
        }

        public void validate() throws JsonParseException {
            if (this.type == null)
                throw new JsonParseException("Missing type for result");

            switch (this.type) {
                case BLOCK -> {
                    if (this._block == null)
                        throw new JsonParseException("Missing block for block result");
                    ResourceLocation blockRL = ResourceLocation.tryParse(_block);
                    if (blockRL == null)
                        throw new JsonParseException("Invalid block for block result");
                    this.block = ForgeRegistries.BLOCKS.getValue(blockRL).defaultBlockState();
                }
                case EXPLOSION -> {
                    if (this.explosionPower == null)
                        throw new JsonParseException("Missing power for explosion result");
                    if (this.shouldGenerateFire == null)
                        this.shouldGenerateFire = false;
                }
                case FUNCTION -> {
                    if (this._function == null)
                        throw new JsonParseException("Missing function for function result");
                    this.function = new CommandFunction.CacheableFunction(new ResourceLocation(this._function));
                }
            }

            if (chance == null)
                chance = 1f;
        }

        public void execute(ServerLevel level, BlockPos pos) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            if (level.getRandom().nextFloat() > this.chance)
                return;

            switch (type) {
                case BLOCK -> level.setBlockAndUpdate(pos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, block));
                case EXPLOSION -> level.explode(null, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, explosionPower, this.shouldGenerateFire, Explosion.BlockInteraction.BREAK);
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

        for (Direction fluidDirection : LiquidBlock.POSSIBLE_FLOW_DIRECTIONS) {
            BlockPos posFluidDirection = pos.relative(fluidDirection);
            if (level.getFluidState(posFluidDirection).getType() != Fluids.EMPTY) {
                if (!cfm.blockToTransform.matchesFluid(level.getFluidState(posFluidDirection).getType()))
                    continue;
            }
            else if (!cfm.blockToTransform.matchesBlock(level.getBlockState(posFluidDirection).getBlock()))
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
