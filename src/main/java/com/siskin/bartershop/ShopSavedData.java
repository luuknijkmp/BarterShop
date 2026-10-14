
package com.siskin.bartershop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

public class ShopSavedData extends SavedData {

    public static final Codec<ShopSavedData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(

                    ShopData.CODEC.listOf()
                            .fieldOf("shops")
                            .forGetter(data -> data.shops)

            ).apply(instance, ShopSavedData::new));


    public static final SavedDataType<ShopSavedData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            "bartershop",
                            "shops"
                    ),
                    ShopSavedData::new,
                    CODEC,
                    DataFixTypes.LEVEL
            );

    private final List<ShopData> shops;

    public ShopSavedData() {
        this.shops = new ArrayList<>();
    }

    private ShopSavedData(List<ShopData> shops) {
        this.shops = new ArrayList<>(shops);
    }

    public List<ShopData> getShops() {
        return List.copyOf(shops);
    }

    public boolean addShop(ShopData shop) {

        for (ShopData existing : shops) {
            if (existing.dimension().equals(shop.dimension())
                    && (existing.chestPos().equals(shop.chestPos())
                    || existing.signPos().equals(shop.signPos()))) {
                return false;
            }
        }

        shops.add(shop);
        setDirty();
        return true;
    }

    public void removeShop(ShopData shop) {
        if (shops.remove(shop)) {
            setDirty();
        }
    }
}
