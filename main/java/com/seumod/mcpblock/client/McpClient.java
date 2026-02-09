package com.seumod.mcpblock.client;

import com.seumod.mcpblock.client.gui.McpScreen;
import com.seumod.mcpblock.network.McpRequestPayload;
import com.seumod.mcpblock.network.McpResponsePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class McpClient {
    public static void sendText(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        ClientPlayNetworking.send(new McpRequestPayload(text));
    }

    public static void displayResponse(String response) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        client.execute(() -> {
            McpScreen screen = McpScreen.getInstance();
            if (client.currentScreen == screen) {
                screen.setResponse(response);
            } else {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("MCP: " + response), false);
                }
            }
        });
    }

    public static void registerPacketReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(McpResponsePayload.ID, (payload, context) -> {
            String response = payload.text();
            displayResponse(response);
        });
    }
}
