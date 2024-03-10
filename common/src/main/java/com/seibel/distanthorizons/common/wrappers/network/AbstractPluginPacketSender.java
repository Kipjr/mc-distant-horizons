package com.seibel.distanthorizons.common.wrappers.network;

import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IPluginPacketSender;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractPluginPacketSender implements IPluginPacketSender
{
	public static final ResourceLocation PLUGIN_CHANNEL_RESOURCE = new ResourceLocation(ModInfo.RESOURCE_NAMESPACE, ModInfo.PLUGIN_CHANNEL_PATH);
	
	
	@Override
	public final void sendPluginPacket(@Nullable IServerPlayerWrapper serverPlayer, ByteBuf buffer)
	{
		FriendlyByteBuf mcBuffer = new FriendlyByteBuf(buffer);
		
		if (serverPlayer != null)
		{
			this.sendPluginPacketServer((ServerPlayer) serverPlayer.getWrappedMcObject(), mcBuffer);
		}
		else
		{
			this.sendPluginPacketClient(mcBuffer);
		}
	}
	
	protected abstract void sendPluginPacketServer(ServerPlayer serverPlayer, FriendlyByteBuf mcBuffer);
	
	protected abstract void sendPluginPacketClient(FriendlyByteBuf mcBuffer);
	
}
