package com.seibel.distanthorizons.forge;

import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.common.wrappers.network.AbstractPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

#if MC_VER >= MC_1_20_2
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import org.apache.commons.lang3.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
#elif MC_VER >= MC_1_18_2
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
#elif MC_VER >= MC_1_17_1
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
#else // < 1.17.1
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.network.PacketDistributor;
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
	public static void setPacketHandler(BiConsumer<IServerPlayerWrapper, ByteBuf> consumer)
	{
		#if MC_VER >= MC_1_20_2
		PLUGIN_CHANNEL.messageBuilder(FriendlyByteBuf.class, 0)
				.encoder((buffer, mcBuffer) -> mcBuffer.writeBytes(buffer))
				.decoder(FriendlyByteBuf::new)
				.consumerNetworkThread((buffer, context) ->
				{
					if (context.getSender() != null)
					{
						consumer.accept(ServerPlayerWrapper.getWrapper(context.getSender()), buffer);
					}
					else
					{
						consumer.accept(null, buffer);
					}
					context.setPacketHandled(true);
				})
				.add();
		#else // < 1.20.2
		PLUGIN_CHANNEL.registerMessage(0, FriendlyByteBuf.class,
				(buffer, mcBuffer) -> mcBuffer.writeBytes(buffer),
				FriendlyByteBuf::new,
				(buffer, context) ->
				{
					if (context.get().getSender() != null)
					{
						consumer.accept(ServerPlayerWrapper.getWrapper(context.get().getSender()), buffer);
					}
					else
					{
						consumer.accept(null, buffer);
					}
					context.get().setPacketHandled(true);
				}
		);
		#endif
	}
	
	@Override
	protected void sendPluginPacketClient(FriendlyByteBuf buffer)
	{
		#if MC_VER >= MC_1_20_2
		PLUGIN_CHANNEL.send(buffer, PacketDistributor.SERVER.noArg());
		#else // < 1.20.2
		PLUGIN_CHANNEL.send(PacketDistributor.SERVER.noArg(), buffer);
		#endif
	}
	
	@Override
	protected void sendPluginPacketServer(ServerPlayer serverPlayer, FriendlyByteBuf buffer)
	{
		#if MC_VER >= MC_1_20_2
		PLUGIN_CHANNEL.send(buffer, PacketDistributor.PLAYER.with(serverPlayer));
		#else // < 1.20.2
		PLUGIN_CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), buffer);
		#endif
	}
	
}
