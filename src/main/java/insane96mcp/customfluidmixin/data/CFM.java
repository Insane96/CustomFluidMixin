package insane96mcp.customfluidmixin.data;

import com.google.gson.annotations.SerializedName;
import insane96mcp.customfluidmixin.exception.JsonValidationException;
import insane96mcp.insanelib.util.IdTagMatcher;
import net.minecraft.commands.CommandFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

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

    public void validate() throws JsonValidationException {
        if (type == null)
            throw new JsonValidationException("Missing type");

        if (_flowing == null)
            throw new JsonValidationException("Missing flowing block");
        flowing = IdTagMatcher.parseLine(_flowing);
        if (flowing == null)
            throw new JsonValidationException("Failed to parse flowing");

        if (type == Type.BLOCK_TRANSFORM) {
            if (_blockToTransform == null)
                throw new JsonValidationException("Missing block_to_transform block");
            blockToTransform = IdTagMatcher.parseLine(_blockToTransform);
            if (blockToTransform == null)
                throw new JsonValidationException("Failed to parse block_to_transform");
        }

        if (_blocksNearby == null)
            throw new JsonValidationException("Missing blocks_nearby");
        int minBlocksNearby = this.type == Type.FLOWING_MIXIN ? 1 : 0;
        if (_blocksNearby.size() < minBlocksNearby || _blocksNearby.size() > 5)
            throw new JsonValidationException("Invalid blocks_nearby. There must be at least one block nearby (0 works too if type = 'block_transform') and less than 5 blocks");

        blocksNearby = new ArrayList<>();
        for (String s : _blocksNearby) {
            IdTagMatcher idTagMatcher = IdTagMatcher.parseLine(s);
            if (idTagMatcher == null)
                throw new JsonValidationException("Failed to parse a block nearby " + s);
            blocksNearby.add(idTagMatcher);
        }

        if (result == null)
            throw new JsonValidationException("Missing result");
        result.validate();

        if (this.fizz == null)
            this.fizz = true;
    }

    public static CFM createFlowingMixin(String flowing, List<IdTagMatcher> blocksNearby, String blockResult) {
        CFM cfm = new CFM();
        cfm.type = Type.FLOWING_MIXIN;
        cfm.flowing = new IdTagMatcher(null, new ResourceLocation(flowing));
        cfm.blocksNearby = blocksNearby;
        cfm.result = MixinResult.newBlockResult(blockResult);
        return cfm;
    }

    public static CFM createBlockTransformation(String flowing, IdTagMatcher blockToTransform, List<IdTagMatcher> blocksNearby, String blockResult) {
        CFM cfm = new CFM();
        cfm.type = Type.BLOCK_TRANSFORM;
        cfm.flowing = new IdTagMatcher(null, new ResourceLocation(flowing));
        cfm.blockToTransform = blockToTransform;
        cfm.blocksNearby = blocksNearby;
        cfm.result = MixinResult.newBlockResult(blockResult);
        return cfm;
    }

    /**
     * Returns a list with all the fluid stacks in the "flowing" key
     */
    public List<FluidStack> getFlowingStacks() {
        return getFluidStacks(this.flowing);
    }

    /**
     * Returns a list with all the fluids stacks in the "block_to_transform" key
     */
    public List<FluidStack> getFluidToTransformStacks() {
        return getFluidStacks(this.blockToTransform);
    }

    /**
     * Returns a list with all the block stacks in the "block_to_transform" key
     */
    public List<ItemStack> getBlockToTransformStacks() {
        return getItemStacks(this.blockToTransform);
    }

    public static List<FluidStack> getFluidStacks(IdTagMatcher idTagMatcher) {
        List<FluidStack> fluidStacks = new ArrayList<>();
        if (idTagMatcher.id != null) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(idTagMatcher.id);
            if (!(fluid instanceof EmptyFluid))
                fluidStacks.add(new FluidStack(fluid, 1000));
        }
        else {
            TagKey<Fluid> fluidTagKey = TagKey.create(Registry.FLUID_REGISTRY, idTagMatcher.tag);
            ITag<Fluid> fluidITag = ForgeRegistries.FLUIDS.tags().getTag(fluidTagKey);
            fluidITag.stream().forEach(fluid -> fluidStacks.add(new FluidStack(fluid, 1000)));
        }
        return fluidStacks;
    }

    public static List<ItemStack> getItemStacks(IdTagMatcher idTagMatcher) {
        List<ItemStack> itemStacks = new ArrayList<>();
        if (idTagMatcher.id != null) {
            Block block = ForgeRegistries.BLOCKS.getValue(idTagMatcher.id);
            itemStacks.add(new ItemStack(block));
        }
        else {
            TagKey<Block> blockTagKey = TagKey.create(Registry.BLOCK_REGISTRY, idTagMatcher.tag);
            ITag<Block> blockITag = ForgeRegistries.BLOCKS.tags().getTag(blockTagKey);
            blockITag.stream().forEach(block -> itemStacks.add(new ItemStack(block)));
        }
        return itemStacks;
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

        public void validate() throws JsonValidationException {
            if (this.type == null)
                throw new JsonValidationException("Missing type for result");

            switch (this.type) {
                case BLOCK -> {
                    if (this._block == null)
                        throw new JsonValidationException("Missing block for block result");
                    ResourceLocation blockRL = ResourceLocation.tryParse(_block);
                    if (blockRL == null)
                        throw new JsonValidationException("Invalid block for block result");
                    this.block = ForgeRegistries.BLOCKS.getValue(blockRL).defaultBlockState();
                }
                case EXPLOSION -> {
                    if (this.explosionPower == null)
                        throw new JsonValidationException("Missing power for explosion result");
                    if (this.shouldGenerateFire == null)
                        this.shouldGenerateFire = false;
                }
                case FUNCTION -> {
                    if (this._function == null)
                        throw new JsonValidationException("Missing function for function result");
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
}
