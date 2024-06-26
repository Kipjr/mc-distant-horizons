package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.AbstractPluginPacketSender;
import com.seibel.distanthorizons.core.network.messages.NetworkMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

#if MC_VER >= MC_1_20_6
import com.seibel.distanthorizons.common.CommonPacketPayload;
#endif

public class FabricPluginPacketSender extends AbstractPluginPacketSender
{
	@Override
	public void sendPluginPacketClient(NetworkMessage message)
	{
		#if MC_VER >= MC_1_20_6
		ClientPlayNetworking.send(new CommonPacketPayload(message));
		#else
		FriendlyByteBuf buffer = PacketByteBufs.create();
		AbstractPluginPacketSender.encodeMessage(buffer, message);
		ClientPlayNetworking.send(PLUGIN_CHANNEL_RESOURCE, buffer);
		#endif
	}
	
	@Override
	public void sendPluginPacketServer(ServerPlayer serverPlayer, NetworkMessage message)
	{
		#if MC_VER >= MC_1_20_6
		ServerPlayNetworking.send(serverPlayer, new CommonPacketPayload(message));
		#else
		FriendlyByteBuf buffer = PacketByteBufs.create();
		AbstractPluginPacketSender.encodeMessage(buffer, message);
		ServerPlayNetworking.send(serverPlayer, PLUGIN_CHANNEL_RESOURCE, buffer);
		#endif
	}
	
}