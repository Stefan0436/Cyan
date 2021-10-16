package modkit.network.channels;

import java.util.function.Supplier;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.game.GameSide;

import modkit.events.objects.network.ClientConnectionEventObject;
import modkit.events.objects.network.ServerConnectionEventObject;
import modkit.network.ByteFlow;
import modkit.network.PacketWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 *
 * Channel context -- internal system
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class PacketChannelContext extends CyanComponent {

	protected static PacketChannelContext clientImplementation;
	protected static PacketChannelContext serverImplementation;
	protected static PacketChannelContext coreImplementation;

	protected static PacketChannelContext getForClient(Supplier<Minecraft> clientSupplier) {
		return clientImplementation.setupNew(clientSupplier);
	}

	protected static PacketChannelContext getForServer(ServerPlayer player) {
		return serverImplementation.setupNew(player);
	}

	protected static PacketChannelContext getFor(ClientConnectionEventObject clientConnection) {
		return clientImplementation.setupNew(clientConnection);
	}

	protected static PacketChannelContext getFor(ServerConnectionEventObject serverConnection) {
		return serverImplementation.setupNew(serverConnection);
	}

	protected abstract PacketChannelContext setupNew(ServerConnectionEventObject serverConnection);

	protected abstract PacketChannelContext setupNew(ClientConnectionEventObject clientConnection);

	protected abstract PacketChannelContext setupNew(Supplier<Minecraft> clientSupplier);

	protected abstract PacketChannelContext setupNew(ServerPlayer server);

	protected abstract Connection getConnection();

	protected abstract Player getPlayer();

	protected abstract String getRemoteBrand();

	protected abstract GameSide getSide();

	protected abstract Level getLevel();

	protected abstract void sendPacket(String channel, String id, PacketWriter writer, boolean allowSplit);

	protected abstract PacketWriter newPacketWriter();

	protected abstract <T extends AbstractPacketProcessor> void registerProcessor(Class<T> processor);

	protected interface PrepareCall {
		public boolean call(PacketChannel channel, AbstractPacketProcessor proc);
	}

	protected interface ProcessCall {
		public boolean call(PacketChannel channel, AbstractPacketProcessor proc, String id, ByteFlow flow);
	}

	protected abstract boolean runProcessor(PacketChannel channel, String id, ByteFlow flow, PrepareCall prepare,
			ProcessCall process);

	public static PacketChannelContext getCore() {
		return coreImplementation;
	}

	protected void findChannels() {
	}

	protected <T extends PacketChannel> T getChannel(Class<T> type, PacketChannelContext context) {
		return null;
	}

	protected PacketChannel[] getChannels(String id, PacketChannelContext context) {
		return null;
	}

	protected void disconnectClient() {
	}

	protected Supplier<Minecraft> getClientSupplier(Object connection) {
		return null;
	}

	public String getClientBrand(ServerPlayer player) {
		return null;
	}
}
