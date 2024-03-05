package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.wrappers.network.AbstractPluginPacketSender;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class FabricPluginPacketSender extends AbstractPluginPacketSender
{
	@Override
	protected void sendPluginPacketClient(FriendlyByteBuf mcBuffer)
	{
		ClientPlayNetworking.send(PLUGIN_CHANNEL_RESOURCE, mcBuffer);
	}
	
	@Override
	protected void sendPluginPacketServer(ServerPlayer serverPlayer, FriendlyByteBuf mcBuffer)
	{
		ServerPlayNetworking.send(serverPlayer, PLUGIN_CHANNEL_RESOURCE, mcBuffer);
	}
	
}
