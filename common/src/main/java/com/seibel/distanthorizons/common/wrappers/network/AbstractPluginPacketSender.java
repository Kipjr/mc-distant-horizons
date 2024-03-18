package com.seibel.distanthorizons.common.wrappers.network;

import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class AbstractPluginPacketSender implements IPluginPacketSender
{
	public static final ResourceLocation PLUGIN_CHANNEL_RESOURCE = new ResourceLocation(ModInfo.RESOURCE_NAMESPACE, ModInfo.PLUGIN_CHANNEL_PATH);
	
	
	@Override
	public final void sendPluginPacket(@Nullable IServerPlayerWrapper serverPlayer, Consumer<ByteBuf> encoder)
	{
		FriendlyByteBuf buffer = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
		
		if (this.shouldAddForgePacketId())
		{
			buffer.writeByte(0);
		}
		
		encoder.accept(buffer);
		
		if (serverPlayer != null)
		{
			this.sendPluginPacketServer((ServerPlayer) serverPlayer.getWrappedMcObject(), buffer);
		}
		else
		{
			this.sendPluginPacketClient(buffer);
		}
	}
	
	protected boolean shouldAddForgePacketId()
	{
		return false;
	}
	
	protected abstract void sendPluginPacketServer(ServerPlayer serverPlayer, FriendlyByteBuf buffer);
	protected abstract void sendPluginPacketClient(FriendlyByteBuf buffer);
	
}
