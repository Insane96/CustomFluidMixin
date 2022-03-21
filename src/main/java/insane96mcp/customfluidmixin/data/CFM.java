package insane96mcp.customfluidmixin.data;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import insane96mcp.customfluidmixin.exception.JsonValidationException;

public class CFM {
    
    @SerializedName("flowing")
    private String _flowing;
    @SerializedName("blocks_nearby")
    private List<String> _blocksNearby;

    public transient IdTagMatcher flowing;
    public transient List<IdTagMatcher> blocksNearby;

    public MixinResult result;

    public void validate() throws JsonValidationException {

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
