package com.seibel.distanthorizons.common;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.logging.ConfigBasedLogger;
import com.seibel.distanthorizons.core.network.event.internal.ProtocolErrorEvent;
import com.seibel.distanthorizons.core.network.messages.MessageRegistry;
import com.seibel.distanthorizons.core.network.messages.NetworkMessage;
import com.seibel.distanthorizons.core.network.messages.base.CloseReasonMessage;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.buffer.ByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public abstract class AbstractPluginPacketSender implements IPluginPacketSender
{
	private static final ConfigBasedLogger LOGGER = new ConfigBasedLogger(LogManager.getLogger(),
			() -> Config.Client.Advanced.Logging.logNetworkEvent.get());
	
	#if MC_VER >= MC_1_21
	public static final ResourceLocation WRAPPER_PACKET_RESOURCE = ResourceLocation.fromNamespaceAndPath(ModInfo.RESOURCE_NAMESPACE, ModInfo.WRAPPER_PACKET_PATH);
	#elif MC_VER >= MC_1_20_6
	public static final ResourceLocation WRAPPER_PACKET_RESOURCE = new ResourceLocation(ModInfo.RESOURCE_NAMESPACE, ModInfo.WRAPPER_PACKET_PATH);
	#else
	public static final ResourceLocation PLUGIN_CHANNEL_RESOURCE = new ResourceLocation(ModInfo.RESOURCE_NAMESPACE, ModInfo.PLUGIN_CHANNEL_PATH);
	#endif
	
	
	@Override
	public final void sendPluginPacketServer(IServerPlayerWrapper serverPlayer, NetworkMessage message)
	{
		this.sendPluginPacketServer((ServerPlayer) serverPlayer.getWrappedMcObject(), message);
	}
	
	@Override
	public abstract void sendPluginPacketClient(NetworkMessage message);
	public abstract void sendPluginPacketServer(ServerPlayer serverPlayer, NetworkMessage message);
	
	@Nullable
	public static NetworkMessage decodeMessage(FriendlyByteBuf in)
	{
		NetworkMessage message = null;
		
		try
		{
			if (in.readShort() != ModInfo.PROTOCOL_VERSION)
			{
				return null;
			}
			
			message = MessageRegistry.INSTANCE.createMessage(in.readUnsignedShort());
			message.decode(in);
			
			if (in.isReadable())
			{
				throw new IOException("Buffer has not been fully read");
			}
			
			return message;
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to decode message", e);
			LOGGER.error("Buffer: {}", in);
			LOGGER.error("Buffer contents: [{}]", ByteBufUtil.hexDump(in));
			
			return new ProtocolErrorEvent(e, message, true);
		}
		finally
		{
			// Prevent connection crashing if not entire buffer has been read
			in.readerIndex(in.writerIndex());
		}
	}
	
	public static void encodeMessage(FriendlyByteBuf out, NetworkMessage message)
	{
		// This is intentionally unhandled, because errors related to this are unlikely to appear in wild
		Objects.requireNonNull(message);
		out.writeShort(ModInfo.PROTOCOL_VERSION);
		
		try
		{
			out.markWriterIndex();
			out.writeShort(MessageRegistry.INSTANCE.getMessageId(message));
			message.encode(out);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to encode message", e);
			LOGGER.error("Message: {}", message);
			
			message.getSession().tryHandleMessage(new ProtocolErrorEvent(e, message, false));
			
			// Encode close reason message instead
			out.resetWriterIndex();
			message = new CloseReasonMessage("Internal error on other side");
			out.writeShort(MessageRegistry.INSTANCE.getMessageId(message));
			message.encode(out);
		}
	}
	
}