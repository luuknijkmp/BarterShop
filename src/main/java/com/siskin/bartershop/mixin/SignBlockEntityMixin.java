
package com.siskin.bartershop.mixin;

import com.siskin.bartershop.ShopData;
import com.siskin.bartershop.ShopManager;
import com.siskin.bartershop.ShopSignParser;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.network.FilteredText;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import com.siskin.bartershop.ShopLanguage;

import java.util.List;
import java.util.UUID;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin {

    @Shadow
    public abstract boolean isWaxed();

    @Shadow
    @Nullable
    public abstract UUID getPlayerWhoMayEdit();

    // Remember whether Minecraft allows this edit.
    @Unique
    private boolean bartershop$authorizedEdit = false;

    // Runs BEFORE Minecraft updates the sign.
    @Inject(
            method = "updateSignText",
            at = @At("HEAD")
    )
    private void bartershop$beforeSignEdit(
            Player player,
            boolean frontText,
            List<FilteredText> lines,
            CallbackInfo ci
    ) {
        SignBlockEntity sign = (SignBlockEntity) (Object) this;

        bartershop$authorizedEdit =
                frontText
                        && !this.isWaxed()
                        && player.getUUID().equals(
                        this.getPlayerWhoMayEdit()
                )
                        && sign.getLevel() != null
                        && player instanceof ServerPlayer;
    }

    // Runs AFTER Minecraft updates the sign.
    @Inject(
            method = "updateSignText",
            at = @At("TAIL")
    )
    private void bartershop$afterSignEdit(
            Player player,
            boolean frontText,
            List<FilteredText> lines,
            CallbackInfo ci
    ) {
        if (!bartershop$authorizedEdit) {
            return;
        }

        // Reset immediately.
        bartershop$authorizedEdit = false;

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        SignBlockEntity sign = (SignBlockEntity) (Object) this;

        String firstLine = sign.getFrontText()
                .getMessage(0, false)
                .getString()
                .trim();

        if (!firstLine.equalsIgnoreCase("[SHOP]")) {
            return;
        }

        // Validate the trade.
        if (ShopSignParser.parse(sign) == null) {
            bartershop$markInvalid(sign, serverPlayer);
            serverPlayer.sendSystemMessage(
                    Component.literal("§c" + ShopLanguage.translate("message.bartershop.invalid_trade")),
                    true
            );
            return;
        }

        BlockState state = sign.getBlockState();

        if (!(state.getBlock() instanceof WallSignBlock)) {
            return;
        }

        var facing = state.getValue(WallSignBlock.FACING);

        BlockPos chestPos = sign.getBlockPos()
                .relative(facing.getOpposite());

        var container = serverPlayer.level().getBlockEntity(chestPos);

        if (!(container instanceof ChestBlockEntity)
                && !(container instanceof BarrelBlockEntity)) {
            serverPlayer.sendSystemMessage(
                    Component.literal("§c" + ShopLanguage.translate("message.bartershop.invalid_container")),
                    true
            );
            return;
        }


        ShopData shop = new ShopData(
                serverPlayer.getUUID(),
                serverPlayer.getGameProfile().name(),
                serverPlayer.level().dimension(),
                sign.getBlockPos(),
                chestPos
        );

        if (!(sign.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!ShopManager.registerShop(serverLevel, shop)) {
            bartershop$markInvalid(sign, serverPlayer);

            serverPlayer.sendSystemMessage(
                    Component.literal("§c" + ShopLanguage.translate("message.bartershop.already_registered")),
                    true
            );
            return;
        }



        // Automatically insert the creator's username.

        // Automatically style the shop sign.
        String ownerName =
                serverPlayer.getGameProfile().name();

        sign.updateText(
                text -> text
                        .setMessage(
                                0,
                                Component.literal("[SHOP]")
                                        .withStyle(ChatFormatting.BOLD)
                        )
                        .setMessage(
                                1,
                                Component.literal(
                                        sign.getFrontText()
                                                .getMessage(1, false)
                                                .getString()
                                ).withStyle(ChatFormatting.GREEN)
                        )
                        .setMessage(
                                2,
                                Component.literal(
                                        sign.getFrontText()
                                                .getMessage(2, false)
                                                .getString()
                                ).withStyle(ChatFormatting.AQUA)
                        )
                        .setMessage(
                                3,
                                Component.literal(ownerName)
                                        .withStyle(ChatFormatting.ITALIC)
                        ),
                true
        );


        serverPlayer.level().sendBlockUpdated(
                sign.getBlockPos(),
                state,
                state,
                3
        );

        serverPlayer.sendSystemMessage(
                Component.literal("§a" + ShopLanguage.translate("message.bartershop.shop_created")),
                true
        );
    }


    @Unique
    private void bartershop$markInvalid(
            SignBlockEntity sign,
            ServerPlayer player
    ) {
        sign.updateText(
                text -> text.setMessage(
                        3,
                        Component.literal(ShopLanguage.translate("message.bartershop.sign_not_registered"))
                                .withStyle(ChatFormatting.RED)
                ),
                true
        );

        BlockState state = sign.getBlockState();

        player.level().sendBlockUpdated(
                sign.getBlockPos(),
                state,
                state,
                3
        );
    }

}
