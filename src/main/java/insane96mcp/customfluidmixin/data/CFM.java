package insane96mcp.customfluidmixin.data;

import com.google.gson.annotations.SerializedName;
import insane96mcp.customfluidmixin.CustomFluidMixin;
import insane96mcp.customfluidmixin.exception.JsonValidationException;
import insane96mcp.insanelib.utils.IdTagMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class CFM {
    
    @SerializedName("flowing")
    private String _flowing;
    @SerializedName("blocks_nearby")
    private List<String> _blocksNearby;

    public MixinResult result;

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
            throw new JsonValidationException("Invalid blocks_nearby. There must be at least one block nearby and less than 5 blocks");
        
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
    }

	@Override
	public String toString() {
		return String.format("CFM[flowing: %s, blocks_nearby: %s, result: %s]", this.flowing, this.blocksNearby, this.result);
	}


    public static class MixinResult {
        public Type type;
        @SerializedName("block")
        private String _block;
        @SerializedName("power")
        public Float explosionPower;
        public String entity;
        @SerializedName("nbt")
        private String _nbt;

        public transient BlockState block;
        public transient CompoundTag nbt;

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
                }
                case SUMMON -> {
                    if (this.entity == null)
                        throw new JsonValidationException("Missing entity for summon result");
                    if (this._nbt != null) {
                        nbt = new CompoundTag();
                        try {
                            nbt = TagParser.parseTag(this._nbt);
                        }
                        catch (Exception e) {
                            throw new JsonValidationException("Failed to parse nbt for summon result");
                        }
                    }
                }
            }
        }

        public void execute(ServerLevel level, BlockPos pos) {
             switch (type) {
                 case BLOCK -> {
                     level.setBlockAndUpdate(pos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, block));
                 }
                 case EXPLOSION -> {
                     level.setBlockAndUpdate(pos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, Blocks.AIR.defaultBlockState()));
                     level.explode(null, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, explosionPower, Explosion.BlockInteraction.BREAK);
                 }
                 case SUMMON -> {
                     level.setBlockAndUpdate(pos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, Blocks.AIR.defaultBlockState()));
                     CompoundTag compoundTag = nbt.copy();
                     compoundTag.putString("id", this.entity);
                     Entity entity = EntityType.loadEntityRecursive(compoundTag, level, (e) -> {
                         e.moveTo(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, e.getYRot(), e.getXRot());
                         return e;
                     });
                     if (entity == null) {
                         CustomFluidMixin.LOGGER.warn("Failed to create entity for Custom Fluid Mixin result");
                         return;
                     }

                     if (this.nbt.isEmpty() && entity instanceof Mob) {
                         ((Mob)entity).finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.TRIGGERED, null, null);
                     }

                     if (!level.tryAddFreshEntityWithPassengers(entity))
                         CustomFluidMixin.LOGGER.warn("Failed to summon entity for Custom Fluid Mixin result");
                 }
             }
        }

        public enum Type {
            @SerializedName("block")
            BLOCK,
            @SerializedName("explosion")
            EXPLOSION,
            @SerializedName("summon")
            SUMMON
        }
    }
}
