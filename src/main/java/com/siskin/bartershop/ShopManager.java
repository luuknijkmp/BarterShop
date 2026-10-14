
package com.siskin.bartershop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class ShopManager {

    private static ShopSavedData getData(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(ShopSavedData.TYPE);
    }

    public static boolean registerShop(
            ServerLevel level,
            ShopData shop
    ) {
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
            if (shop.dimension().equals(level.dimension())
                    && shop.chestPos().equals(chestPos)) {
                return shop;
            }
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
