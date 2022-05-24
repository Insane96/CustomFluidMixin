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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

import java.util.ArrayList;
import java.util.List;

public class CFM {

    @SerializedName("flowing")
    private String _flowing;
    @SerializedName("blocks_nearby")
    private List<String> _blocksNearby;

    public MixinResult result;

    public Boolean fizz;

    public transient IdTagMatcher flowing;
    public transient List<IdTagMatcher> blocksNearby;

    public void validate() throws JsonValidationException {
        if (_flowing == null)
            throw new JsonValidationException("Missing flowing block");
        flowing = IdTagMatcher.parseLine(_flowing);
        if (flowing == null)
            throw new JsonValidationException("Failed to parse flowing");

        if (_blocksNearby == null)
            throw new JsonValidationException("Missing blocks_nearby");
        if (_blocksNearby.size() == 0 || _blocksNearby.size() > 4)
            throw new JsonValidationException(
                    "Invalid blocks_nearby. There must be at least one block nearby and less than 5 blocks");

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

    public List<List<ItemStack>> getBlocksNearby() {
        List<List<ItemStack>> r = new ArrayList<>();
        for (IdTagMatcher idBlocksNearby : blocksNearby) {
            r.add(getAllBlocksOrFluidsAsStacks(idBlocksNearby));
        }
        return r;
    }

    public List<ItemStack> getFlowingStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        List<Fluid> fluids = this.getAllFluids();
        for (Fluid fluid : fluids) {
            ItemStack stack = new ItemStack(fluid.getBucket());
            stacks.add(stack);
        }
        return stacks;
    }

    public List<ItemStack> getAllBlocksOrFluidsAsStacks(IdTagMatcher idTagMatcher) {
        List<ItemStack> stacks = new ArrayList<>();
        if (idTagMatcher.id != null) {
            Block block = ForgeRegistries.BLOCKS.getValue(idTagMatcher.id);
            if (block != null) {
                stacks.add(new ItemStack(block));
            }
            else {
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(idTagMatcher.id);
                stacks.add(new ItemStack(fluid.getBucket()));
            }

        }
        else {
            TagKey<Block> blockTagKey = TagKey.create(Registry.BLOCK_REGISTRY, idTagMatcher.tag);
            TagKey<Fluid> fluidTagKey = TagKey.create(Registry.FLUID_REGISTRY, idTagMatcher.tag);
            if (ForgeRegistries.BLOCKS.tags().isKnownTagName(blockTagKey)) {
                ITag<Block> blockTag = ForgeRegistries.BLOCKS.tags().getTag(blockTagKey);
                blockTag.stream().forEach(block -> stacks.add(new ItemStack(block)));
            }
            else {
                ITag<Fluid> fluidITag = ForgeRegistries.FLUIDS.tags().getTag(fluidTagKey);
                fluidITag.stream().forEach(fluid -> stacks.add(new ItemStack(fluid.getBucket())));
            }
        }
        return stacks;
    }

    public List<Fluid> getAllFluids() {
        List<Fluid> fluids = new ArrayList<>();
        if (this.flowing.id != null) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(this.flowing.id);
            if (fluid != null)
                fluids.add(fluid);
        }
        else {
            TagKey<Fluid> tagKey = TagKey.create(Registry.FLUID_REGISTRY, this.flowing.tag);
            ITag<Fluid> blockTag = ForgeRegistries.FLUIDS.tags().getTag(tagKey);
            fluids.addAll(blockTag.stream().toList());
        }
        return fluids;
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
            level.setBlockAndUpdate(pos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, Blocks.AIR.defaultBlockState()));
            if (level.getRandom().nextFloat() > this.chance)
                return;

            switch (type) {
                case BLOCK -> {
                    level.setBlockAndUpdate(pos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, block));
                }
                case EXPLOSION -> {
                    level.explode(null, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, explosionPower, this.shouldGenerateFire, Explosion.BlockInteraction.BREAK);
                }
                case FUNCTION -> {
                    MinecraftServer server = level.getServer();
                    this.function.get(server.getFunctions()).ifPresent((commandFunction) -> {
                        server.getFunctions().execute(commandFunction, server.getFunctions().getGameLoopSender().withPosition(new Vec3(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d)).withLevel(level));
                    });
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
}
