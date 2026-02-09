package com.seumod.mcpblock.client.gui;

import com.seumod.mcpblock.client.McpClient;
import com.seumod.mcpblock.network.McpResponsePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class McpGuiMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(McpResponsePayload.ID, (payload, context) -> {
            String response = payload.text();
            McpClient.displayResponse(response);
        });
    }

    public static void openScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen == null) {
            client.setScreen(McpScreen.getInstance());
        }
    }
}
