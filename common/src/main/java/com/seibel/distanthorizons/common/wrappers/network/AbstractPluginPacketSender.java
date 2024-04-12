package com.seibel.distanthorizons.common.wrappers.network;

import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public abstract class AbstractPluginPacketSender implements IPluginPacketSender
{
	public static final ResourceLocation PLUGIN_CHANNEL_RESOURCE = new ResourceLocation(ModInfo.RESOURCE_NAMESPACE, ModInfo.PLUGIN_CHANNEL_PATH);
	public static final ResourceLocation WRAPPER_PACKET_RESOURCE = new ResourceLocation(ModInfo.RESOURCE_NAMESPACE, ModInfo.WRAPPER_PACKET_PATH);
	
	
	@Override
	public final void sendPluginPacketClient(Consumer<ByteBuf> encoder)
	{
		FriendlyByteBuf buffer = this.createBuffer(encoder);
		this.sendPluginPacketClient(buffer);
	}
	
	@Override
	public final void sendPluginPacketServer(IServerPlayerWrapper serverPlayer, Consumer<ByteBuf> encoder)
	{
		FriendlyByteBuf buffer = this.createBuffer(encoder);
		this.sendPluginPacketServer((ServerPlayer) serverPlayer.getWrappedMcObject(), buffer);
	}
	
	private FriendlyByteBuf createBuffer(Consumer<ByteBuf> encoder)
	{
		FriendlyByteBuf buffer = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
		
		if (this.shouldAddForgePacketId())
		{
			buffer.writeByte(0);
		}
		
		encoder.accept(buffer);
		return buffer;
	}
	
	protected boolean shouldAddForgePacketId()
	{
		return false;
	}
	
	protected abstract void sendPluginPacketClient(FriendlyByteBuf buffer);
	protected abstract void sendPluginPacketServer(ServerPlayer serverPlayer, FriendlyByteBuf buffer);
	
}
