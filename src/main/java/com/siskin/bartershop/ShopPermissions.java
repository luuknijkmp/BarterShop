
package com.siskin.bartershop;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

public final class ShopPermissions {

    private static final Identifier ADMIN_PERMISSION =
            Identifier.fromNamespaceAndPath("bartershop", "admin");

    private ShopPermissions() {
    }

    public static boolean isAdmin(ServerPlayer player) {
        return player.checkPermission(
                ADMIN_PERMISSION,
                PermissionLevel.GAMEMASTERS
        );
    }

    public static boolean canManageShop(ServerPlayer player, ShopData shop) {
        return ShopManager.isOwner(shop, player.getUUID())
                || isAdmin(player);
    }
}
