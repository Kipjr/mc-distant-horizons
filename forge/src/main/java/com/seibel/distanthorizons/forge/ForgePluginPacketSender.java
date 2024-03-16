package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.wrappers.network.AbstractPluginPacketSender;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

#if MC_VER >= MC_1_20_2
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
#elif MC_VER >= MC_1_18_2
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
#elif MC_VER >= MC_1_17_1
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
#else // < 1.17.1
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
#endif

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ForgePluginPacketSender extends AbstractPluginPacketSender
{
	public static final SimpleChannel PLUGIN_CHANNEL =
			#if MC_VER >= MC_1_20_2
			ChannelBuilder.named(AbstractPluginPacketSender.PLUGIN_CHANNEL_RESOURCE)
					.networkProtocolVersion(1)
					.serverAcceptedVersions((status, version) -> true)
					.clientAcceptedVersions((status, version) -> true)
					.simpleChannel();
			#else // < 1.20.2
			NetworkRegistry.newSimpleChannel(
					AbstractPluginPacketSender.PLUGIN_CHANNEL_RESOURCE,
					() -> "1",
					ignored -> true,
					ignored -> true
			);
			#endif
	
	public static void setPacketHandler(Consumer<ByteBuf> consumer)
	{
		setPacketHandler((player, buffer) -> consumer.accept(buffer));
	}
	public static void setPacketHandler(BiConsumer<ServerPlayer, ByteBuf> consumer)
	{
		#if MC_VER >= MC_1_20_2
		PLUGIN_CHANNEL.messageBuilder(ByteBuf.class, 0)
				.encoder((buffer, mcBuffer) -> mcBuffer.writeBytes(buffer))
				.decoder(FriendlyByteBuf::asReadOnly)
				.consumerNetworkThread((buffer, context) ->
				{
					consumer.accept(context.getSender(), buffer);
					context.setPacketHandled(true);
				})
				.add();
		#else // < 1.20.2
		PLUGIN_CHANNEL.registerMessage(0, ByteBuf.class,
				// encoder
				(buffer, mcBuffer) -> mcBuffer.writeBytes(buffer),
				// decoder
				FriendlyByteBuf::asReadOnly,
				// message consumer
				(buffer, context) ->
				{
					consumer.accept(context.get().getSender(), buffer);
					context.get().setPacketHandled(true);
				}
		);
		#endif
	}
	
	@Override
	protected void sendPluginPacketClient(ByteBuf buffer)
	{
		#if MC_VER >= MC_1_20_2
		PLUGIN_CHANNEL.send(buffer, PacketDistributor.SERVER.noArg());
		#else // < 1.20.2
		PLUGIN_CHANNEL.send(PacketDistributor.SERVER.noArg(), buffer);
		#endif
	}
	
	@Override
	protected void sendPluginPacketServer(ServerPlayer serverPlayer, ByteBuf buffer)
	{
		#if MC_VER >= MC_1_20_2
		PLUGIN_CHANNEL.send(buffer, PacketDistributor.PLAYER.with(serverPlayer));
		#else // < 1.20.2
		PLUGIN_CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), buffer);
		#endif
	}
	
}
