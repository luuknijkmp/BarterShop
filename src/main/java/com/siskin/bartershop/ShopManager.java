
package com.siskin.bartershop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.core.Direction;

public class ShopManager {

    private static ShopSavedData getData(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(ShopSavedData.TYPE);
    }


    public static boolean registerShop(
            ServerLevel level,
            ShopData shop
    ) {
        // A registered shop already uses this chest inventory,
        // including the opposite half of a double chest.
        if (getShopByChest(level, shop.chestPos()) != null) {
            return false;
        }

        return getData(level).addShop(shop);
    }


    public static ShopData getShopBySign(
            ServerLevel level,
            BlockPos signPos
    ) {
        for (ShopData shop : getData(level).getShops()) {
            if (shop.dimension().equals(level.dimension())
                    && shop.signPos().equals(signPos)) {
                return shop;
            }
        }

        return null;
    }


    public static ShopData getShopByChest(
            ServerLevel level,
            BlockPos chestPos
    ) {
        for (ShopData shop : getData(level).getShops()) {
            if (!shop.dimension().equals(level.dimension())) {
                continue;
            }

            if (shop.chestPos().equals(chestPos)) {
                return shop;
            }

            BlockState anchorState = level.getBlockState(shop.chestPos());

            if (!(anchorState.getBlock() instanceof ChestBlock)) {
                continue;
            }

            ChestType chestType = anchorState.getValue(ChestBlock.TYPE);

            if (chestType == ChestType.SINGLE) {
                continue;
            }

            Direction facing = anchorState.getValue(ChestBlock.FACING);

            Direction otherDirection = chestType == ChestType.LEFT
                    ? facing.getClockWise()
                    : facing.getCounterClockWise();

            BlockPos otherPos = shop.chestPos().relative(otherDirection);

            if (!otherPos.equals(chestPos)) {
                continue;
            }

            BlockState otherState = level.getBlockState(otherPos);

            if (otherState.getBlock() != anchorState.getBlock()) {
                continue;
            }

            if (otherState.getValue(ChestBlock.FACING) != facing) {
                continue;
            }

            if (otherState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
                continue;
            }

            return shop;
        }

        return null;
    }


    public static boolean isOwner(
            ShopData shop,
            UUID playerUuid
    ) {
        return shop.ownerUuid().equals(playerUuid);
    }

    public static void removeShop(
            ServerLevel level,
            BlockPos signPos
    ) {
        ShopData shop = getShopBySign(level, signPos);

        if (shop != null) {
            getData(level).removeShop(shop);
        }
    }
}
