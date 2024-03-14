package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.wrappers.network.AbstractPluginPacketSender;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class FabricPluginPacketSender extends AbstractPluginPacketSender
{
	@Override
	protected void sendPluginPacketClient(ByteBuf buffer)
	{
		FriendlyByteBuf mcBuffer = new FriendlyByteBuf(PooledByteBufAllocator.DEFAULT.buffer());
		
		// (Neo)Forge packet ID (Unused, always expected to be 0)
		mcBuffer.writeByte(0);
		
		mcBuffer.writeBytes(buffer);
		ClientPlayNetworking.send(PLUGIN_CHANNEL_RESOURCE, mcBuffer);
	}
	
	@Override
	protected void sendPluginPacketServer(ServerPlayer serverPlayer, ByteBuf buffer)
	{
		FriendlyByteBuf mcBuffer = new FriendlyByteBuf(buffer);
		
		// (Neo)Forge packet ID (Unused, always expected to be 0)
		mcBuffer.writeByte(0);
		
		mcBuffer.writeBytes(buffer);
		ServerPlayNetworking.send(serverPlayer, PLUGIN_CHANNEL_RESOURCE, mcBuffer);
	}
	
}
