package com.seibel.distanthorizons.common;

import com.seibel.distanthorizons.core.network.messages.PluginMessageRegistry;
import com.seibel.distanthorizons.core.network.plugin.PluginChannelMessage;
import com.seibel.distanthorizons.core.network.protocol.INetworkObject;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class AbstractPluginPacketSender implements IPluginPacketSender
{
	public static final ResourceLocation PLUGIN_CHANNEL_RESOURCE = new ResourceLocation(ModInfo.RESOURCE_NAMESPACE, ModInfo.PLUGIN_CHANNEL_PATH);
	public static final ResourceLocation WRAPPER_PACKET_RESOURCE = new ResourceLocation(ModInfo.RESOURCE_NAMESPACE, ModInfo.WRAPPER_PACKET_PATH);
	
	
	@Override
	public final void sendPluginPacketServer(IServerPlayerWrapper serverPlayer, PluginChannelMessage message)
	{
		this.sendPluginPacketServer((ServerPlayer) serverPlayer.getWrappedMcObject(), message);
	}
	
	@Override public abstract void sendPluginPacketClient(PluginChannelMessage message);
	public abstract void sendPluginPacketServer(ServerPlayer serverPlayer, PluginChannelMessage message);
	
	@Nullable
	public static PluginChannelMessage decodeMessage(FriendlyByteBuf in)
	{
		if (in.readShort() != ModInfo.PROTOCOL_VERSION)
		{
			return null;
		}
		
		PluginChannelMessage message = PluginMessageRegistry.INSTANCE.createMessage(in.readUnsignedShort());
		return INetworkObject.decodeToInstance(message, in);
	}
	
	public static void encodeMessage(FriendlyByteBuf out, PluginChannelMessage message)
	{
		Objects.requireNonNull(message);
		
		out.writeShort(ModInfo.PROTOCOL_VERSION);
		
		out.writeShort(PluginMessageRegistry.INSTANCE.getMessageId(message));
		message.encode(out);
	}
	
}