
package com.siskin.bartershop;

import net.minecraft.world.item.Item;

public record ShopTrade(
        Item sellingItem,
        int sellingAmount,
        Item paymentItem,
        int paymentAmount
) {
}
