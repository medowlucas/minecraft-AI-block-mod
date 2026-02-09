package com.seumod.mcpblock.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record McpRequestPayload(String text) implements CustomPayload {
    public static final CustomPayload.Id<McpRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mcpblock", "mcp_request"));

    public static final PacketCodec<RegistryByteBuf, McpRequestPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, McpRequestPayload::text,
                    McpRequestPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
