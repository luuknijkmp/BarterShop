
package com.siskin.bartershop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record ShopData(
        UUID ownerUuid,
        String ownerName,
        ResourceKey<Level> dimension,
        BlockPos signPos,
        BlockPos chestPos
) {

    public static final Codec<ShopData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(

                    Codec.STRING.fieldOf("owner_uuid")
                            .xmap(UUID::fromString, UUID::toString)
                            .forGetter(ShopData::ownerUuid),

                    Codec.STRING.fieldOf("owner_name")
                            .forGetter(ShopData::ownerName),

                    Identifier.CODEC.fieldOf("dimension")
                            .xmap(
                                    id -> ResourceKey.create(
                                            Registries.DIMENSION, id
                                    ),
                                    ResourceKey::identifier
                            )
                            .forGetter(ShopData::dimension),

                    BlockPos.CODEC.fieldOf("sign_pos")
                            .forGetter(ShopData::signPos),

                    BlockPos.CODEC.fieldOf("chest_pos")
                            .forGetter(ShopData::chestPos)

            ).apply(instance, ShopData::new));
}
