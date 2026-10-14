
package com.siskin.bartershop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShopTransaction {

    public static String execute(
            ServerPlayer buyer,
            Container chest,
            ShopTrade trade
    ) {
        Container inventory = buyer.getInventory();

        // Work with copies so failed trades change nothing.
        List<ItemStack> chestCopy = snapshot(chest);
        List<ItemStack> playerCopy = snapshot(inventory).subList(0, 36);

        if (!remove(chestCopy, trade.sellingItem(),
                trade.sellingAmount())) {
            return "Shop is out of stock!";
        }

        if (!remove(playerCopy, trade.paymentItem(),
                trade.paymentAmount())) {
            return "You don't have enough payment items!";
        }

        if (!insert(chestCopy, trade.paymentItem(),
                trade.paymentAmount(), chest.getMaxStackSize())) {
            return "Shop chest has no room for payment!";
        }

        if (!insert(playerCopy, trade.sellingItem(),
                trade.sellingAmount(), inventory.getMaxStackSize())) {
            return "Your inventory is full!";
        }

        // All checks passed. Commit both inventories.
        commit(chest, chestCopy);
        commit(inventory, playerCopy);

        return null; // Success
    }

    private static List<ItemStack> snapshot(Container container) {
        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            result.add(container.getItem(i).copy());
        }

        return result;
    }

    private static boolean remove(
            List<ItemStack> slots, Item item, int amount
    ) {
        int available = 0;

        for (ItemStack stack : slots) {
            if (stack.is(item)) {
                available += stack.getCount();
            }
        }

        if (available < amount) return false;

        for (int i = 0; i < slots.size() && amount > 0; i++) {
            ItemStack stack = slots.get(i);

            if (!stack.is(item)) continue;

            int taken = Math.min(amount, stack.getCount());
            stack.shrink(taken);
            amount -= taken;
        }

        return true;
    }

    private static boolean insert(
            List<ItemStack> slots,
            Item item,
            int amount,
            int containerLimit
    ) {
        // First fill existing stacks.
        for (ItemStack stack : slots) {
            if (!stack.is(item)) continue;

            int limit = Math.min(
                    stack.getMaxStackSize(), containerLimit
            );

            int space = Math.max(0, limit - stack.getCount());
            int added = Math.min(space, amount);

            stack.grow(added);
            amount -= added;

            if (amount == 0) return true;
        }

        // Then fill empty slots.
        for (int i = 0; i < slots.size() && amount > 0; i++) {
            if (!slots.get(i).isEmpty()) continue;

            ItemStack stack = new ItemStack(item);
            int added = Math.min(
                    amount,
                    Math.min(stack.getMaxStackSize(), containerLimit)
            );

            stack.setCount(added);
            slots.set(i, stack);
            amount -= added;

            if (amount == 0) return true;
        }

        return false;
    }

    private static void commit(
            Container container, List<ItemStack> slots
    ) {
        for (int i = 0; i < slots.size(); i++) {
            container.setItem(i, slots.get(i));
        }

        container.setChanged();
    }
}
