package insane96mcp.customfluidmixin.data;

import com.google.gson.annotations.SerializedName;
import insane96mcp.customfluidmixin.exception.JsonValidationException;
import insane96mcp.insanelib.utils.IdTagMatcher;
import net.minecraft.resources.ResourceLocation;
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


    public class MixinResult {
        public Type type;
        @SerializedName("block")
        private String _block;
        @SerializedName("power")
        public Float explosionPower;
        public String entity;
        public String nbt;

        public transient BlockState block;

        public void validate() throws JsonValidationException {
            if (type == null)
                throw new JsonValidationException("Missing type for result");

            switch (type) {
                case BLOCK -> {
                    if (_block == null)
                        throw new JsonValidationException("Missing block for block result");
                    ResourceLocation blockRL = ResourceLocation.tryParse(_block);
                    if (blockRL == null)
                        throw new JsonValidationException("Invalid block for block result");
                    block = ForgeRegistries.BLOCKS.getValue(blockRL).defaultBlockState();
                }
                case EXPLOSION -> {
                    if (explosionPower == null)
                        throw new JsonValidationException("Missing power for explosion result");
                }
                case SUMMON -> {
                    throw new JsonValidationException("Not yet implemented");
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
