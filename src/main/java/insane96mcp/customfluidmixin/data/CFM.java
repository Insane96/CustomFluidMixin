package insane96mcp.customfluidmixin.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import insane96mcp.customfluidmixin.CustomFluidMixin;
import insane96mcp.customfluidmixin.exception.JsonValidationException;
import insane96mcp.insanelib.utils.IdTagMatcher;
import net.minecraft.commands.CommandFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

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
        public String entity;
        @SerializedName("nbt")
        private String _nbt;
        @SerializedName("function")
        private String _function;
        public Float chance;

        public transient BlockState block;
        public transient CompoundTag nbt;
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
                case SUMMON -> {
                    if (this.entity == null)
                        throw new JsonValidationException("Missing entity for summon result");
                    if (this._nbt != null) {
                        try {
                            nbt = TagParser.parseTag(this._nbt);
                        } catch (Exception e) {
                            throw new JsonValidationException("Failed to parse nbt for summon result");
                        }
                    }
                }
                //TODO Test, if works remove Summon
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
                case SUMMON -> {
                    CompoundTag compoundTag = new CompoundTag();
                    if (this.nbt != null)
                        compoundTag = this.nbt.copy();
                    compoundTag.putString("id", this.entity);
                    Entity entity = EntityType.loadEntityRecursive(compoundTag, level, (e) -> {
                        e.moveTo(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, e.getYRot(), e.getXRot());
                        return e;
                    });
                    if (entity == null) {
                        CustomFluidMixin.LOGGER.warn("Failed to create entity for Custom Fluid Mixin result");
                        return;
                    }

                    if (this.nbt != null && this.nbt.isEmpty() && entity instanceof Mob) {
                        ((Mob) entity).finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.TRIGGERED, null, null);
                    }

                    if (!level.tryAddFreshEntityWithPassengers(entity))
                        CustomFluidMixin.LOGGER.warn("Failed to summon entity for Custom Fluid Mixin result");
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
            @SerializedName("summon")
            SUMMON,
            @SerializedName("function")
            FUNCTION
        }
    }
}
