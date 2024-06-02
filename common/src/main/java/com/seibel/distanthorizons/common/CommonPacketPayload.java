package com.seibel.distanthorizons.common;

import com.seibel.distanthorizons.core.network.messages.PluginMessageRegistry;
import com.seibel.distanthorizons.core.network.plugin.PluginChannelMessage;
import com.seibel.distanthorizons.core.network.protocol.INetworkObject;
import com.seibel.distanthorizons.coreapi.ModInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CommonPacketPayload implements CustomPacketPayload
{
	public static final Type<CommonPacketPayload> TYPE = new Type<>(AbstractPluginPacketSender.WRAPPER_PACKET_RESOURCE);
	
	@NotNull
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
	
	@Nullable
	public PluginChannelMessage message;
	
	
	public CommonPacketPayload(@Nullable PluginChannelMessage message)
	{
		this.message = message;
	}
	
	
	public static class Codec implements StreamCodec<FriendlyByteBuf, CommonPacketPayload>
	{
		@NotNull
		@Override
		public CommonPacketPayload decode(@NotNull FriendlyByteBuf in)
		{
			if (in.readShort() != ModInfo.PROTOCOL_VERSION)
			{
				return new CommonPacketPayload(null);
			}
			
			PluginChannelMessage message = PluginMessageRegistry.INSTANCE.createMessage(in.readUnsignedShort());
			return new CommonPacketPayload(INetworkObject.decodeToInstance(message, in));
		}
		
		@Override
		public void encode(@NotNull FriendlyByteBuf out, CommonPacketPayload payload)
		{
			Objects.requireNonNull(payload.message);
			
			out.writeShort(ModInfo.PROTOCOL_VERSION);
			
			out.writeShort(PluginMessageRegistry.INSTANCE.getMessageId(payload.message));
			payload.message.encode(out);
		}
		
	}
	
}