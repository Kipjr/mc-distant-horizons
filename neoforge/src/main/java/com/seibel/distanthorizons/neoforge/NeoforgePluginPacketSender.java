package com.seibel.distanthorizons.neoforge;

import com.seibel.distanthorizons.common.wrappers.misc.ServerPlayerWrapper;
import com.seibel.distanthorizons.common.wrappers.network.AbstractPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NeoforgePluginPacketSender extends AbstractPluginPacketSender
{
	private static BiConsumer<IServerPlayerWrapper, ByteBuf> packetConsumer;
	
	public static void setPacketHandler(RegisterPayloadHandlerEvent event, Consumer<ByteBuf> consumer)
	{
		setPacketHandler(event, (player, buffer) -> consumer.accept(buffer));
	}
	public static void setPacketHandler(RegisterPayloadHandlerEvent event, BiConsumer<IServerPlayerWrapper, ByteBuf> consumer)
	{
		packetConsumer = consumer;
		IPayloadRegistrar registrar = event.registrar(ModInfo.RESOURCE_NAMESPACE);
		registrar.play(PacketWrapperPayload.ID, PacketWrapperPayload::createFrom, handler -> handler
						.client(NeoforgePluginPacketSender::handlePacket)
						.server(NeoforgePluginPacketSender::handlePacket))
				.optional();
	}
	
	public static void handlePacket(PacketWrapperPayload data, PlayPayloadContext context)
	{
		ServerPlayerWrapper serverPlayer = context.player()
				.map(player -> player instanceof ServerPlayer ? (ServerPlayer) player : null)
				.map(ServerPlayerWrapper::getWrapper)
				.orElse(null);
		packetConsumer.accept(serverPlayer, data.buffer);
	}
	
	@Override
	protected void sendPluginPacketClient(FriendlyByteBuf buffer)
	{
		PacketDistributor.SERVER.noArg().send(new PacketWrapperPayload(buffer));
	}
	
	@Override
	protected void sendPluginPacketServer(ServerPlayer serverPlayer, FriendlyByteBuf buffer)
	{
		PacketDistributor.PLAYER.with(serverPlayer).send(new PacketWrapperPayload(buffer));
	}
	
	
	public record PacketWrapperPayload(
			ByteBuf buffer
	) implements CustomPacketPayload
	{
		public static final ResourceLocation ID = AbstractPluginPacketSender.WRAPPER_PACKET_RESOURCE;
		
		public static PacketWrapperPayload createFrom(FriendlyByteBuf buffer)
		{
			return new PacketWrapperPayload(buffer.readBytes(buffer.readableBytes()));
		}
		
		@Override
		@NotNull
		public ResourceLocation id()
		{
			return ID;
		}
		
		@Override
		public void write(FriendlyByteBuf out)
		{
			this.buffer.resetReaderIndex();
			out.writeBytes(this.buffer);
		}
		
	}
	
}
