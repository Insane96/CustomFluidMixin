package insane96mcp.customfluidmixin.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import insane96mcp.customfluidmixin.exception.JsonValidationException;

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
        
        blocksNearby = new ArrayList<IdTagMatcher>();
        for (String s : _blocksNearby) {
            IdTagMatcher idTagMatcher = IdTagMatcher.parseLine(s);
            if (idTagMatcher == null)
                throw new JsonValidationException("Failed to parse flowing");
        }
        blocksNearby = IdTagMatcher.parseStringList();
        if (blocksNearby.size() == 0 || blocksNearby.size() > 4)
            throw new JsonValidationException("Failed to parse blocks_nearby. There must be at least one block nearby and less than 5 blocks");

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
        public String block;
        @SerializedName("power")
        public Float explosionPower;
        public String entity;
        public String nbt;

        public void validate() throws JsonValidationException {

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
