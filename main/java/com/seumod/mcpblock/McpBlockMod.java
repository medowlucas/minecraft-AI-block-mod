package com.seumod.mcpblock;

import com.seumod.mcpblock.block.McpBlock;
import com.seumod.mcpblock.network.McpPackets;
import com.seumod.mcpblock.network.McpRequestPayload;
import com.seumod.mcpblock.network.McpResponsePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class McpBlockMod implements ModInitializer {
    public static final String MOD_ID = "mcpblock";

    private static final RegistryKey<Block> MCP_BLOCK_KEY =
            RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "mcp_block"));
    private static final RegistryKey<Item> MCP_BLOCK_ITEM_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "mcp_block"));

    public static final Block MCP_BLOCK = new McpBlock(
            AbstractBlock.Settings.create().registryKey(MCP_BLOCK_KEY));
    public static final BlockItem MCP_BLOCK_ITEM = new BlockItem(
            MCP_BLOCK, new Item.Settings().registryKey(MCP_BLOCK_ITEM_KEY).useBlockPrefixedTranslationKey());

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, MCP_BLOCK_KEY, MCP_BLOCK);
        Registry.register(Registries.ITEM, MCP_BLOCK_ITEM_KEY, MCP_BLOCK_ITEM);

        PayloadTypeRegistry.playC2S().register(McpRequestPayload.ID, McpRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(McpResponsePayload.ID, McpResponsePayload.CODEC);

        McpPackets.registerPackets();
    }
}
