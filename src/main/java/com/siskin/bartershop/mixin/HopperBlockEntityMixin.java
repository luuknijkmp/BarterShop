
package com.siskin.bartershop.mixin;

import com.siskin.bartershop.ShopData;
import com.siskin.bartershop.ShopGameRules;
import com.siskin.bartershop.ShopManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(
            method = "suckInItems",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void bartershop$preventShopExtraction(
            Level level,
            Hopper hopper,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.getGameRules()
                .get(ShopGameRules.SHOP_PROTECTION)) {
            return;
        }

        BlockPos sourcePos = BlockPos.containing(
                hopper.getLevelX(),
                hopper.getLevelY() + 1.0,
                hopper.getLevelZ()
        );

        ShopData shop = ShopManager.getShopByChest(
                serverLevel,
                sourcePos
        );

        if (shop != null) {
            cir.setReturnValue(false);
        }
    }
}
