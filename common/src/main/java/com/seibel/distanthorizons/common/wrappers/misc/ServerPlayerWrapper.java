package com.seibel.distanthorizons.common.wrappers.misc;

import com.google.common.base.Objects;
import com.google.common.collect.MapMaker;
import com.seibel.distanthorizons.common.wrappers.world.ServerLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.core.util.math.Vec3d;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;

/**
 * This wrapper transparently ensures that underlying {@link ServerPlayer} is always valid,
 * unless the player has disconnected.
 */
public class ServerPlayerWrapper implements IServerPlayerWrapper
{
	private static final ConcurrentMap<ServerGamePacketListenerImpl, ServerPlayerWrapper> serverPlayerWrapperMap = new MapMaker().weakKeys().weakValues().makeMap();
	
	private final ServerGamePacketListenerImpl connection;
	private ServerPlayer serverPlayer()
	{
		return this.connection.player;
	}
	
	public static ServerPlayerWrapper getWrapper(ServerPlayer serverPlayer)
	{
		return serverPlayerWrapperMap.computeIfAbsent(serverPlayer.connection, ignored -> new ServerPlayerWrapper(serverPlayer.connection));
	}
	
	private ServerPlayerWrapper(ServerGamePacketListenerImpl connection)
	{
		this.connection = connection;
	}
	
	
	@Override
	public String getName()
	{
		return this.serverPlayer().getName().getString();
	}
	
	@Override
	public IServerLevelWrapper getLevel()
	{
		#if MC_VER < MC_1_20_1
		return ServerLevelWrapper.getWrapper(this.serverPlayer().getLevel());
		#else
		return ServerLevelWrapper.getWrapper(this.serverPlayer().serverLevel());
		#endif
	}
	
	@Override
	public Vec3d getPosition()
	{
		Vec3 position = this.serverPlayer().position();
		return new Vec3d(position.x, position.y, position.z);
	}
	
	@Override
	public int getViewDistance()
	{
		return this.serverPlayer().server.getPlayerList().getViewDistance();
	}
	
	@Override
	public SocketAddress getRemoteAddress()
	{
		#if MC_VER >= MC_1_19_4
		return this.serverPlayer().connection.getRemoteAddress();
		#else // < 1.19.4
		return this.serverPlayer().connection.connection.getRemoteAddress();
		#endif
	}
	
	@Override
	public Object getWrappedMcObject()
	{
		return this.serverPlayer();
	}
	
	@Override
	public String toString()
	{
		return "Wrapped{" + this.serverPlayer() + "}";
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof ServerPlayerWrapper))
		{
			return false;
		}
		ServerPlayerWrapper that = (ServerPlayerWrapper) o;
		return Objects.equal(this.connection, that.connection);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.connection);
	}
	
}