package com.seumod.mcpblock.block;

import com.seumod.mcpblock.client.gui.McpGuiMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class McpBlock extends Block {
    public McpBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            McpGuiMod.openScreen();
            return ActionResult.SUCCESS;
        }

        player.sendMessage(net.minecraft.text.Text.literal("MCP Block clicked! Position: " + pos.toString()), false);
        return ActionResult.SUCCESS;
    }
}
