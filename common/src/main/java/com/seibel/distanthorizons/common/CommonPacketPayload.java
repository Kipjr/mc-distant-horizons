package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.AbstractPluginPacketSender;
import com.seibel.distanthorizons.core.network.messages.PluginMessageRegistry;
import com.seibel.distanthorizons.core.network.plugin.PluginChannelMessage;
import com.seibel.distanthorizons.core.network.protocol.INetworkObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FabricPacketPayload implements CustomPacketPayload
{
	public static final Type<FabricPacketPayload> TYPE = new Type<>(AbstractPluginPacketSender.WRAPPER_PACKET_RESOURCE);
	
	@NotNull
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
	
	@Nullable
	public PluginChannelMessage message;
	
	
	public FabricPacketPayload(@Nullable PluginChannelMessage message)
	{
		this.message = message;
	}
	
	
	public static class Codec implements StreamCodec<FriendlyByteBuf, FabricPacketPayload>
	{
		@NotNull
		@Override
		public FabricPacketPayload decode(FriendlyByteBuf in)
		{
			return new FabricPacketPayload(
					INetworkObject.decodeToInstance(PluginMessageRegistry.INSTANCE.createMessage(in.readUnsignedShort()), in)
			);
		}
		
		@Override
		public void encode(FriendlyByteBuf out, FabricPacketPayload payload)
		{
			Objects.requireNonNull(payload.message).encode(out);
		}
		
	}
	
}