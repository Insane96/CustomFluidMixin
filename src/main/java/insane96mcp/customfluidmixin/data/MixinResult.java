package insane96mcp.customfluidmixin.data;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import insane96mcp.customfluidmixin.CustomFluidMixin;
import insane96mcp.insanelib.util.weightedrandom.IWeightedRandom;
import insane96mcp.insanelib.util.weightedrandom.WeightedRandom;
import net.minecraft.commands.CommandFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@JsonAdapter(MixinResult.Serializer.class)
public class MixinResult {
    public Type type;
    public float explosionPower;
    public Boolean shouldGenerateFire;
    public float chance;
    public List<BlockResult> blocks;
    public CommandFunction.CacheableFunction function;

    public static class Serializer implements JsonDeserializer<MixinResult>, JsonSerializer<MixinResult> {
        @Override
        public MixinResult deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            MixinResult mixinResult = new MixinResult();
            JsonObject jObject = json.getAsJsonObject();
            mixinResult.type = context.deserialize(jObject.get("type"), Type.class);
            switch (mixinResult.type) {
                case BLOCK -> {
                    if (jObject.has("blocks") && jObject.get("blocks").isJsonArray())
                        mixinResult.blocks = context.deserialize(jObject.get("blocks"), BlockResult.LIST_TYPE);
                    else
                        mixinResult.blocks = List.of(new BlockResult(GsonHelper.getAsString(jObject, "block"), 1));
                    if (mixinResult.blocks.isEmpty())
                        throw new JsonParseException("blocks must contain at least one entry");
                }
                case EXPLOSION -> {
                    mixinResult.explosionPower = GsonHelper.getAsFloat(jObject, "explosion_power");
                    mixinResult.shouldGenerateFire = GsonHelper.getAsBoolean(jObject, "fire", false);
                }
                case FUNCTION ->
                        mixinResult.function = new CommandFunction.CacheableFunction(new ResourceLocation(GsonHelper.getAsString(jObject, "function")));
            }
            mixinResult.chance = GsonHelper.getAsFloat(jObject, "chance", 1f);
            return mixinResult;
        }

        @Override
        public JsonElement serialize(MixinResult mixinResult, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jObject = new JsonObject();
            jObject.add("type", context.serialize(mixinResult.type));
            switch (mixinResult.type) {
                case BLOCK -> jObject.add("block", context.serialize(mixinResult.blocks));
                case EXPLOSION -> {
                    jObject.addProperty("explosion_power", mixinResult.explosionPower);
                    if (mixinResult.shouldGenerateFire)
                        jObject.addProperty("fire", true);
                }
                case FUNCTION -> jObject.add("function", context.serialize(mixinResult.function.getId()));
            }
            jObject.addProperty("chance", mixinResult.chance);

            return jObject;
        }
    }

    public static MixinResult newBlockResult(String block) {
        MixinResult m = new MixinResult();
        m.type = Type.BLOCK;
        m.blocks = List.of(new BlockResult(block, 1));
        return m;
    }

    public void execute(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        if (level.getRandom().nextFloat() > this.chance)
            return;

        switch (this.type) {
            case BLOCK -> {
                BlockResult randomBlockResult = this.getRandomBlockResult(level.random);
                if (randomBlockResult == null) {
                    CustomFluidMixin.LOGGER.warn("No random block found for Custom Fluid Mixin");
                    break;
                }
                level.setBlockAndUpdate(pos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, pos, randomBlockResult.getState()));
            }
            case EXPLOSION ->
                    level.explode(null, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, explosionPower, this.shouldGenerateFire, Level.ExplosionInteraction.BLOCK);
            case FUNCTION -> {
                MinecraftServer server = level.getServer();
                this.function.get(server.getFunctions()).ifPresent((commandFunction) -> server.getFunctions().execute(commandFunction, server.getFunctions().getGameLoopSender().withPosition(new Vec3(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d)).withLevel(level)));
            }
        }
    }

    @Nullable
    public BlockResult getRandomBlockResult(RandomSource random) {
        if (this.blocks == null || this.blocks.isEmpty())
            return null;
        return WeightedRandom.getRandomItem(random, this.blocks);
    }

    public enum Type {
        @SerializedName("block")
        BLOCK,
        @SerializedName("explosion")
        EXPLOSION,
        @SerializedName("function")
        FUNCTION
    }

    @JsonAdapter(BlockResult.Serializer.class)
    public static class BlockResult implements IWeightedRandom {
        private final BlockState block;
        private final int weight;

        public static final java.lang.reflect.Type LIST_TYPE = new TypeToken<ArrayList<BlockResult>>(){}.getType();

        public static class Serializer implements JsonDeserializer<BlockResult>, JsonSerializer<BlockResult> {
            @Override
            public BlockResult deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject jObject = json.getAsJsonObject();
                //TODO Change to deserialize the full state
                return new BlockResult(ForgeRegistries.BLOCKS.getValue(context.deserialize(jObject.get("block"), ResourceLocation.class)).defaultBlockState(), GsonHelper.getAsInt(jObject, "weight", 1));
            }

            @Override
            public JsonElement serialize(BlockResult blockResult, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
                JsonObject jObject = new JsonObject();
                jObject.addProperty("block", ForgeRegistries.BLOCKS.getKey(blockResult.block.getBlock()).toString());
                jObject.addProperty("weight", blockResult.weight);
                return jObject;
            }
        }

        public BlockResult(BlockState block, int weight) {
            this.block = block;
            this.weight = weight;
        }

        public BlockResult(String block, int weight) {
            this.block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(block)).defaultBlockState();
            this.weight = weight;
        }

        public BlockState getState() {
            return block;
        }

        @Override
        public int getWeight() {
            return this.weight;
        }
    }
}
