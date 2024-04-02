package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.wrappers.network.AbstractPluginPacketSender;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class FabricPluginPacketSender extends AbstractPluginPacketSender
{
	@Override
	protected boolean shouldAddForgePacketId()
	{
		return true;
	}
	
	@Override
	protected void sendPluginPacketClient(FriendlyByteBuf buffer)
	{
		ClientPlayNetworking.send(PLUGIN_CHANNEL_RESOURCE, buffer);
	}
	
	@Override
	protected void sendPluginPacketServer(ServerPlayer serverPlayer, FriendlyByteBuf buffer)
	{
		ServerPlayNetworking.send(serverPlayer, PLUGIN_CHANNEL_RESOURCE, buffer);
	}
	
}
