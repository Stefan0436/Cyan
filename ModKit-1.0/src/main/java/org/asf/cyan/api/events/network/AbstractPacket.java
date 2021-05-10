package org.asf.cyan.api.events.network;

import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.PacketWriter;
import org.asf.cyan.api.network.channels.PacketChannel;

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
