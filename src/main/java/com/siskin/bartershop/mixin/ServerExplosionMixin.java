
package com.siskin.bartershop.mixin;

import com.siskin.bartershop.ShopGameRules;
import com.siskin.bartershop.ShopManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.siskin.bartershop.ShopData;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Inject(
            method = "interactWithBlocks",
            at = @At("HEAD")
    )

    private void bartershop$protectShopBlocks(
            List<BlockPos> targetBlocks,
            CallbackInfo ci
    ) {
        if (!level.getGameRules().get(ShopGameRules.SHOP_PROTECTION)) {
            return;
        }

        targetBlocks.removeIf(pos ->
                ShopManager.getShopByChest(level, pos) != null
                        || ShopManager.getShopBySign(level, pos) != null
        );
    }


    @Inject(
            method = "interactWithBlocks",
            at = @At("TAIL")
    )

    private void bartershop$cleanupDestroyedShops(
            List<BlockPos> targetBlocks,
            CallbackInfo ci
    ) {
        if (level.getGameRules().get(ShopGameRules.SHOP_PROTECTION)) {
            return;
        }

        for (BlockPos pos : targetBlocks) {
            ShopData shop = ShopManager.getShopBySign(level, pos);

            if (shop == null) {
                shop = ShopManager.getShopByChest(level, pos);
            }

            if (shop == null) {
                continue;
            }

            boolean signDestroyed =
                    level.getBlockState(shop.signPos()).isAir();

            boolean chestDestroyed =
                    level.getBlockState(shop.chestPos()).isAir();

            if (signDestroyed || chestDestroyed) {
                ShopManager.removeShop(level, shop.signPos());
            }
        }
    }

}
