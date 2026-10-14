
package com.siskin.bartershop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class ShopSignParser {

    public static ShopTrade parse(SignBlockEntity sign) {

        String sellingLine = sign.getFrontText()
                .getMessage(1, false)
                .getString()
                .trim();

        String paymentLine = sign.getFrontText()
                .getMessage(2, false)
                .getString()
                .trim();

        ParsedItem selling = parseItem(sellingLine);
        ParsedItem payment = parseItem(paymentLine);

        if (selling == null || payment == null) {
            return null;
        }

        return new ShopTrade(
                selling.item(),
                selling.amount(),
                payment.item(),
                payment.amount()
        );
    }

    private static ParsedItem parseItem(String line) {

        String[] parts = line.split("\\s+", 2);

        if (parts.length != 2) {
            return null;
        }

        int amount;

        try {
            amount = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (amount <= 0) {
            return null;
        }

        Identifier id = Identifier.tryParse(parts[1]);

        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }

        Item item = BuiltInRegistries.ITEM.getValue(id);

        return new ParsedItem(item, amount);
    }

    private record ParsedItem(Item item, int amount) {
    }
}
