package com.seumod.mcpblock.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record McpResponsePayload(String text) implements CustomPayload {
    public static final CustomPayload.Id<McpResponsePayload> ID =
            new CustomPayload.Id<>(Identifier.of("mcpblock", "mcp_response"));

    public static final PacketCodec<RegistryByteBuf, McpResponsePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, McpResponsePayload::text,
                    McpResponsePayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
