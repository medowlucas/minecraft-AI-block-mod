package com.seumod.mcpblock.network;

import java.util.regex.Pattern;

import com.seumod.mcpblock.service.McpHttpClientV2;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class McpPackets {
    // Security: Command whitelist regex - only allow give and sumon/summon
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^CMD:\\s*(give|sumon|summon)\\s");

    public static void registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(McpRequestPayload.ID, (payload, context) -> {
            String text = payload.text();
            ServerPlayerEntity player = context.player();
            MinecraftServer server = context.server();

            new Thread(() -> {
                try {
                    String playerName = player.getName().getString();
                    System.out.println("[McpBlock] Querying LMStudio for: " + text + " (player: " + playerName + ")");
String response = McpHttpClientV2.queryLlm(playerName, text);

                    if (response == null || response.isEmpty()) {
                        final String chatResponse = "Desculpe, nao consegui obter uma resposta.";
                        server.execute(() -> {
                            player.sendMessage(Text.literal("§7[MCP] §f" + chatResponse), false);
                            // Send response to client GUI
                            ServerPlayNetworking.send(player, new McpResponsePayload(chatResponse));
                        });
                        return;
                    }

                    System.out.println("[McpBlock] Got response: " + response);

                    final String finalResponse = response.trim();
                    server.execute(() -> {
                        processAiResponse(finalResponse, playerName, server, player);
                        // Always send response to client GUI
                        ServerPlayNetworking.send(player, new McpResponsePayload(finalResponse));
                    });
                } catch (Exception e) {
                    System.err.println("[McpBlock] Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }, "McpBlock-LLM-Query").start();
        });
    }

    /**
     * Process AI response following the security spec:
     * - If starts with CMD: -> validate and execute command
     * - Otherwise -> send as chat message
     */
    private static void processAiResponse(String response, String playerName, MinecraftServer server, ServerPlayerEntity player) {
        if (!response.startsWith("CMD:")) {
            System.out.println("[McpBlock] Chat response: " + response);
            player.sendMessage(Text.literal("§b[MCP] §f" + response), false);
            return;
        }

        if (!COMMAND_PATTERN.matcher(response).find()) {
            System.err.println("[McpBlock] BLOCKED: Invalid command pattern - " + response);
            player.sendMessage(Text.literal("§c[Seguranca] §fComando bloqueado: padrao invalido."), false);
            return;
        }

        String command = response.substring(4).trim();

        if (!isCommandSafe(command, playerName)) {
            System.err.println("[McpBlock] BLOCKED: Dangerous command - " + command);
            player.sendMessage(Text.literal("§c[Seguranca] §fComando bloqueado: nao permitido."), false);
            return;
        }

        // Process summon with relative coordinates (~)
        command = processRelativeCoordinates(command, player);

        System.out.println("[McpBlock] Executing validated command: " + command);
        try {
            server.getCommandManager().getDispatcher().execute(command, player.getCommandSource());
            player.sendMessage(Text.literal("§a[OK] §f" + command), false);
        } catch (Exception e) {
            System.err.println("[McpBlock] Command execution error: " + e.getMessage());
            player.sendMessage(Text.literal("§c[Erro] §fFalha ao executar comando."), false);
        }
    }

/**
     * Process relative coordinates (~) in summon commands
     * ~ -> 3 blocks in front of player
     * ~ ~ ~ -> 3 blocks in front, same height
     * ~ ~5 ~ -> 3 blocks in front, 5 above player
     * Calculates "in front of player" based on yaw rotation
     */
private static String processRelativeCoordinates(String command, ServerPlayerEntity player) {
        if (!command.toLowerCase().startsWith("summon") && !command.toLowerCase().startsWith("sumon")) {
            return command;
        }
        
        if (!command.contains("~")) {
            return command;
        }

        // Get player's position and rotation
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        float yaw = player.getYaw();

        // Calculate "in front" offset (3 blocks in front of player)
        double forward = 3.0;
        double offsetX = Math.sin(Math.toRadians(yaw)) * forward;
        double offsetZ = -Math.cos(Math.toRadians(yaw)) * forward;

        System.out.println("[McpBlock] Player at: " + px + ", " + py + ", " + pz + " yaw: " + yaw);
        System.out.println("[McpBlock] Offset: " + offsetX + ", " + offsetZ);

        // For summon commands, extract the entity and coordinates separately
        // Find the entity part (everything after "summon " or "sumon ")
        String lowerCommand = command.toLowerCase();
        int commandStart = lowerCommand.startsWith("summon") ? 7 : 5; // length of "summon" or "sumon"
        // Skip any additional whitespace after command prefix
        while (commandStart < command.length() && Character.isWhitespace(command.charAt(commandStart))) {
            commandStart++;
        }
        
        if (command.length() <= commandStart) {
            return command;
        }
        
        String afterCommand = command.substring(commandStart);
        System.out.println("[McpBlock] After command prefix: " + afterCommand);
        
        // Extract entity and coordinates
        // The entity is everything before the first ~
        int firstTilde = afterCommand.indexOf('~');
        if (firstTilde == -1) {
            return command;
        }
        
        String entityPart = afterCommand.substring(0, firstTilde).trim();
        String coordsPart = afterCommand.substring(firstTilde);
        
        System.out.println("[McpBlock] Entity: " + entityPart);
        System.out.println("[McpBlock] Coords: " + coordsPart);
        
        // Normalize coordinates: replace ~~~ with ~ ~ ~, ~~ with ~ ~
        coordsPart = coordsPart.replace("~~~", "~ ~ ~");
        coordsPart = coordsPart.replace("~~", "~ ~");
        
        // Split coordinates
        String[] coordParts = coordsPart.split("\\s+");
        
        double coordX = px + offsetX;
        double coordY = py;
        double coordZ = pz + offsetZ;
        
        // Parse each coordinate
        for (int i = 0; i < coordParts.length && i < 3; i++) {
            String coord = coordParts[i];
            if (coord.startsWith("~")) {
                if (coord.length() > 1) {
                    // ~number format
                    double offset = Double.parseDouble(coord.substring(1));
                    if (i == 0) coordX += offset;
                    else if (i == 1) coordY += offset;
                    else if (i == 2) coordZ += offset;
                }
                // else just ~ means keep the relative position
            }
        }
        
        // Build final command
        String finalCommand = "summon " + entityPart + " " + 
            String.format(java.util.Locale.US, "%.1f", coordX) + " " +
            String.format(java.util.Locale.US, "%.1f", coordY) + " " +
            String.format(java.util.Locale.US, "%.1f", coordZ);
        
        System.out.println("[McpBlock] Final command: " + finalCommand);
        return finalCommand;
    }

    /**
     * Validate that command is safe to execute
     * - Must use explicit player name for 'give' (not selectors)
     * - Must not contain dangerous patterns
     */
    private static boolean isCommandSafe(String command, String playerName) {
        String lowerCommand = command.toLowerCase();

        // Check for selectors - NEVER allowed
        if (lowerCommand.contains("@p") || lowerCommand.contains("@a") ||
            lowerCommand.contains("@s") || lowerCommand.contains("@e") ||
            lowerCommand.contains("@r")) {
            System.err.println("[McpBlock] BLOCKED: Selector detected");
            return false;
        }

        // For 'give' commands, verify player name is used explicitly
        if (lowerCommand.startsWith("give")) {
            if (!lowerCommand.contains(playerName.toLowerCase())) {
                System.err.println("[McpBlock] BLOCKED: Player name not found in give command");
                return false;
            }
        }

        return true;
    }
}
