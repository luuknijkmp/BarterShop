
package com.siskin.bartershop;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;

public class ShopGameRules {

    public static final GameRule<Boolean> SHOP_PROTECTION =
            Registry.register(
                    BuiltInRegistries.GAME_RULE,
                    Identifier.fromNamespaceAndPath(
                            "bartershop",
                            "shop_protection"
                    ),
                    new GameRule<>(
                            GameRuleCategory.PLAYER,
                            GameRuleType.BOOL,
                            BoolArgumentType.bool(),
                            GameRuleTypeVisitor::visitBoolean,
                            Codec.BOOL,
                            value -> value ? 1 : 0,
                            true,
                            FeatureFlagSet.of()
                    )
            );

    public static void initialize() {
        // Calling this method initializes the static gamerule.
    }
}
