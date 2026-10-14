
package com.siskin.bartershop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import java.util.ArrayList;
import java.util.List;

public class BarterShop implements ModInitializer {

	private record PendingSignSync(
			ServerPlayer player,
			BlockPos pos
	) {}

	private static final List<PendingSignSync> PENDING_SIGN_SYNCS =
			new ArrayList<>();


	@Override
	public void onInitialize() {

		ShopGameRules.initialize();


		PlayerBlockBreakEvents.BEFORE.register(
				(level, player, pos, state, blockEntity) -> {

					if (level.isClientSide()) {
						return true;
					}

					if (!(player instanceof ServerPlayer serverPlayer)) {
						return true;
					}

					if (!serverPlayer.level().getGameRules()
							.get(ShopGameRules.SHOP_PROTECTION)) {
						return true;
					}

					ShopData shop = ShopManager.getShopBySign(
							serverPlayer.level(),
							pos
					);

					if (shop == null) {
						shop = ShopManager.getShopByChest(
								serverPlayer.level(),
								pos
						);
					}

					if (shop == null) {
						return true;
					}

					if (ShopManager.isOwner(
							shop,
							serverPlayer.getUUID()
					)) {
						return true;
					}


					serverPlayer.sendSystemMessage(
							Component.literal(
									"§c[BarterShop] You cannot break someone else's shop!"
							),
							true
					);

// Restore the block state on the player's client.
					//serverPlayer.connection.send(
					//		new ClientboundBlockUpdatePacket(
					//				serverPlayer.level(),
					//				pos
					//		)
					//);

// Restore block entity data, including the sign text.
					//if (blockEntity != null) {
					//	var packet = blockEntity.getUpdatePacket();

//						if (packet != null) {
//							serverPlayer.connection.send(packet);
//						}
//					}

					if (blockEntity instanceof SignBlockEntity) {
						PENDING_SIGN_SYNCS.add(
								new PendingSignSync(serverPlayer, pos.immutable())
						);
					}

					return false;

				}

		);


		ServerTickEvents.END_SERVER_TICK.register(server -> {

			if (PENDING_SIGN_SYNCS.isEmpty()) {
				return;
			}

			List<PendingSignSync> pending =
					new ArrayList<>(PENDING_SIGN_SYNCS);

			PENDING_SIGN_SYNCS.clear();

			for (PendingSignSync sync : pending) {

				ServerPlayer player = sync.player();
				BlockPos pos = sync.pos();

				if (!(player.level().getBlockEntity(pos)
						instanceof SignBlockEntity sign)) {
					continue;
				}

				player.connection.send(
						new ClientboundBlockUpdatePacket(
								player.level(),
								pos
						)
				);

				var packet = sign.getUpdatePacket();

				if (packet != null) {
					player.connection.send(packet);
				}
			}
		});



		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {

			if (level.isClientSide() || player.isSpectator()) {
				return InteractionResult.PASS;
			}

			if (!(player instanceof ServerPlayer serverPlayer)) {
				return InteractionResult.PASS;
			}

			BlockPos signPos = hitResult.getBlockPos();

			if (!(level.getBlockEntity(signPos)
					instanceof SignBlockEntity sign)) {
				return InteractionResult.PASS;
			}

			// Read the first line on the front of the sign
			String firstLine = sign.getFrontText()
					.getMessage(0, false)
					.getString()
					.trim();

			if (!firstLine.equalsIgnoreCase("[SHOP]")) {
				return InteractionResult.PASS;
			}

			// Verify that this is a wall sign
			BlockState signState = level.getBlockState(signPos);

			if (!(signState.getBlock() instanceof WallSignBlock)) {
				return InteractionResult.PASS;
			}

			// The sign's facing direction points away from
			// the block to which it is attached
			var facing = signState.getValue(WallSignBlock.FACING);
			BlockPos chestPos = signPos.relative(facing.getOpposite());


			if (!(level.getBlockEntity(chestPos)
					instanceof ChestBlockEntity)) {

				serverPlayer.sendSystemMessage(
						Component.literal("§c[BarterShop] Sign must be attached to a chest!"),
						true
				);

				return InteractionResult.SUCCESS;
			}

			ShopData registeredShop = ShopManager.getShopBySign(
					serverPlayer.level(),
					signPos
			);

			if (registeredShop == null) {
				serverPlayer.sendSystemMessage(
						Component.literal(
								"§c[BarterShop] This shop is not registered!"
						),
						true
				);

				return InteractionResult.SUCCESS;
			}


// We now know the sign is attached to a chest.
// Read the trade information from the sign.
			ShopTrade trade = ShopSignParser.parse(sign);

			if (trade == null) {
				serverPlayer.sendSystemMessage(
						Component.literal("§c[BarterShop] Invalid trade format!"),
						true
				);

				return InteractionResult.SUCCESS;
			}

// The shop sign contains a valid trade.
			serverPlayer.sendSystemMessage(
					Component.literal("§a[BarterShop] Shop trade recognized!"),
					true
			);


			ChestBlockEntity chest =
					(ChestBlockEntity) level.getBlockEntity(chestPos);

			String error = ShopTransaction.execute(
					serverPlayer,
					chest,
					trade
			);

			if (error != null) {
				serverPlayer.sendSystemMessage(
						Component.literal("§c[BarterShop] " + error),
						true
				);
			} else {
				serverPlayer.sendSystemMessage(
						Component.literal("§a[BarterShop] Trade successful!"),
						true
				);
			}

			return InteractionResult.SUCCESS;


		});
	}
}
