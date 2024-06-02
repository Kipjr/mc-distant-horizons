package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.AbstractPluginPacketSender;
import com.seibel.distanthorizons.common.CommonPacketPayload;
import com.seibel.distanthorizons.core.network.plugin.PluginChannelMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class FabricPluginPacketSender extends AbstractPluginPacketSender
{
	@Override
	public void sendPluginPacketClient(PluginChannelMessage message)
	{
		ClientPlayNetworking.send(new CommonPacketPayload(message));
	}
	
	@Override
	public void sendPluginPacketServer(ServerPlayer serverPlayer, PluginChannelMessage message)
	{
		ServerPlayNetworking.send(serverPlayer, new CommonPacketPayload(message));
	}
	
}