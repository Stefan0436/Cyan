package modkit.events.network;

import modkit.network.PacketReader;
import modkit.network.PacketWriter;
import modkit.network.channels.PacketChannel;

/**
 * 
 * Packet abstract to help with networking
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 * @param <T> Self
 */
public abstract class AbstractPacket<T extends AbstractPacket<T>> {

	protected abstract String id();

	protected String channel() {
		return "<auto>";
	}

	@SuppressWarnings("unchecked")
	public T read(PacketReader input) {
		readEntries(input);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T write(PacketChannel channel) {
		PacketWriter packet = channel.newPacket();
		String ch = channel();
		if (ch.equals("<auto>"))
			ch = channel.id();

		writeEntries(packet);
		channel.sendPacket(ch, id(), packet);
		return (T) this;
	}

	protected abstract void readEntries(PacketReader reader);

	protected abstract void writeEntries(PacketWriter writer);

}
