package com.fastbuildup.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class FastBuildUpClientMod implements ClientModInitializer {
    public static final ModConfig config = new ModConfig();
    private static KeyMapping toggleKey;
    private static final KeyMapping.Category GENERAL_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("fastbuildup", "general")
    );
    private int cooldown = 0;

    @Override
    public void onInitializeClient() {
        config.load();

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.fastbuildup.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            GENERAL_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null || client.gameMode == null) {
                return;
            }

            // Handle toggle press
            while (toggleKey.consumeClick()) {
                config.enabled = !config.enabled;
                config.save();
                client.player.sendSystemMessage(
                    Component.literal("§7[§6FastBuildUp§7] §fMod is now " + (config.enabled ? "§eEnabled" : "§7Disabled"))
                );
            }

            if (cooldown > 0) {
                cooldown--;
            }

            LocalPlayer player = client.player;

            if (!shouldActivate(client, player)) {
                return;
            }

            if (cooldown <= 0) {
                boolean placed = tryPlaceBlockUnderPlayer(client, player);
                if (placed) {
                    applyVerticalBoost(player);
                    player.swing(InteractionHand.MAIN_HAND);
                    cooldown = config.placementCooldownTicks;
                }
            }
        });
    }

    private boolean shouldActivate(Minecraft client, LocalPlayer player) {
        if (!config.enabled) return false;

        if (player.isSpectator()) return false;
        if (player.getAbilities().flying) return false;
        if (!config.allowInWater && (player.isInWater() || player.isInLava())) return false;
        if (player.isPassenger()) return false;
        if (!player.isAlive()) return false;

        if (config.sneakDisables && player.isCrouching()) return false;

        if (config.requireJump && !client.options.keyJump.isDown()) return false;
        if (config.requireRightClick && !client.options.keyUse.isDown()) return false;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem)) return false;

        return true;
    }

    private boolean tryPlaceBlockUnderPlayer(Minecraft client, LocalPlayer player) {
        Level level = player.level();

        BlockPos targetPos = findBestPlacementPosition(player, level);
        if (targetPos == null) {
            return false;
        }

        BlockPos supportPos = targetPos.below();

        BlockHitResult hitResult = new BlockHitResult(
            new Vec3(supportPos.getX() + 0.5, supportPos.getY() + 1.0, supportPos.getZ() + 0.5),
            Direction.UP,
            supportPos,
            false
        );

        InteractionResult result = client.gameMode.useItemOn(
            player,
            InteractionHand.MAIN_HAND,
            hitResult
        );

        return result.consumesAction();
    }

    private BlockPos findBestPlacementPosition(LocalPlayer player, Level level) {
        double feetY = player.getY();
        int targetY = Mth.floor(feetY - 0.5);

        BlockPos centerPos = new BlockPos(
            Mth.floor(player.getX()),
            targetY,
            Mth.floor(player.getZ())
        );

        if (isValidPlacement(level, centerPos)) {
            return centerPos;
        }

        BlockPos blockPosDown = player.blockPosition().below();
        if (isValidPlacement(level, blockPosDown)) {
            return blockPosDown;
        }

        return null;
    }

    private boolean isValidPlacement(Level level, BlockPos placePos) {
        if (placePos == null) return false;

        BlockState state = level.getBlockState(placePos);
        if (!state.isAir() && !state.is(BlockTags.REPLACEABLE)) {
            return false;
        }

        BlockPos supportPos = placePos.below();
        BlockState supportState = level.getBlockState(supportPos);
        if (supportState.isAir() || supportState.is(BlockTags.REPLACEABLE)) {
            return false;
        }

        return true;
    }

    private void applyVerticalBoost(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        double boost = config.verticalBoost;
        double max = config.maxVerticalSpeed;

        double newY = Math.min(boost, max);

        if (config.centerPlayerOnPillar) {
            double centerX = Mth.floor(player.getX()) + 0.5;
            double centerZ = Mth.floor(player.getZ()) + 0.5;
            double diffX = centerX - player.getX();
            double diffZ = centerZ - player.getZ();
            double pull = 0.1;
            player.setDeltaMovement(new Vec3(velocity.x + diffX * pull, newY, velocity.z + diffZ * pull));
        } else {
            player.setDeltaMovement(new Vec3(velocity.x, newY, velocity.z));
        }
    }
}
