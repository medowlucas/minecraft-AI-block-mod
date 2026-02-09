package com.seumod.mcpblock;

import com.seumod.mcpblock.client.McpClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class McpBlockModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register packet receiver for responses
        McpClient.registerPacketReceiver();
    }
}
