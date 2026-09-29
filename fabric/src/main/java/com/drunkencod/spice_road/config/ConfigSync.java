package com.drunkencod.spice_road.config;

import java.util.HashMap;
import java.util.Map;

import com.drunkencod.spice_road.Constants;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-client payload carrying the current value of every {@code SERVER}
 * config option. NeoForge's built-in {@code ModConfigSpec} sync only reaches
 * NeoForge clients, so Fabric needs its own; NeoForge keeps using its
 * built-in mechanism and never sends this. Sent to each player on join and to
 * everyone after {@code /reload}, mirroring {@code SpiceProfileSync}.
 * <p>
 * Values are keyed by dotted option path and self-describe their type, so a
 * client whose schema doesn't recognize an ID (a mod version mismatch) can
 * still decode past it instead of failing the whole packet.
 *
 * @param values Every SERVER option's current value, keyed by
 *               {@link ConfigOption#getDottedPath()}.
 */
public record ConfigSync(Map<String, Object> values) implements CustomPacketPayload {

    private static final byte TYPE_BOOLEAN = 0;
    private static final byte TYPE_INT = 1;
    private static final byte TYPE_LONG = 2;
    private static final byte TYPE_DOUBLE = 3;

    /** Payload type ID. */
    public static final CustomPacketPayload.Type<ConfigSync> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "config_sync"));

    /** Network codec. */
    public static final StreamCodec<FriendlyByteBuf, ConfigSync> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.values.size());
                payload.values.forEach((path, value) -> {
                    buf.writeUtf(path);
                    writeValue(buf, value);
                });
            },
            buf -> {
                int size = buf.readVarInt();
                Map<String, Object> values = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    values.put(buf.readUtf(), readValue(buf));
                }
                return new ConfigSync(values);
            });

    private static void writeValue(FriendlyByteBuf buf, Object value) {
        if (value instanceof Boolean b) {
            buf.writeByte(TYPE_BOOLEAN);
            buf.writeBoolean(b);
        } else if (value instanceof Integer i) {
            buf.writeByte(TYPE_INT);
            buf.writeInt(i);
        } else if (value instanceof Long l) {
            buf.writeByte(TYPE_LONG);
            buf.writeLong(l);
        } else if (value instanceof Double d) {
            buf.writeByte(TYPE_DOUBLE);
            buf.writeDouble(d);
        } else {
            throw new IllegalArgumentException("Unsupported config value type " + value.getClass());
        }
    }

    private static Object readValue(FriendlyByteBuf buf) {
        byte tag = buf.readByte();
        return switch (tag) {
            case TYPE_BOOLEAN -> buf.readBoolean();
            case TYPE_INT -> buf.readInt();
            case TYPE_LONG -> buf.readLong();
            case TYPE_DOUBLE -> buf.readDouble();
            default -> throw new IllegalArgumentException("Unknown config value tag " + tag);
        };
    }

    /** @return A payload of every SERVER option's current local value. */
    public static ConfigSync current() {
        return new ConfigSync(FabricConfigHelper.currentServerValues());
    }

    /** Applies a received payload on the client. */
    public void handle() {
        ConfigSyncOverride.accept(values);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
