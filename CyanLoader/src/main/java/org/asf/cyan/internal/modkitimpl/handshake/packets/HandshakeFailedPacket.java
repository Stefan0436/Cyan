package org.asf.cyan.internal.modkitimpl.handshake.packets;

import java.util.stream.Stream;

import modkit.events.network.AbstractPacket;
import modkit.network.PacketReader;
import modkit.network.PacketWriter;
import modkit.network.PacketReader.RawReader;
import modkit.network.PacketWriter.RawWriter;

public class HandshakeFailedPacket extends AbstractPacket<HandshakeFailedPacket> {

	public static enum FailureType {
		PROTOCOL_LOCAL(0), PROTOCOL_REMOTE(1), LOADER_LOCAL(2), LOADER_REMOTE(3);

		private int value;

		FailureType(int value) {
			this.value = value;
		}
	}

	public FailureType failure;
	public double version;
	public String language;
	public String displayVersion;

	@Override
	protected String id() {
		return "handshake.failed";
	}

	@Override
	protected void readEntries(PacketReader packet) {
		RawReader reader = new RawReader(packet);
		int type = reader.readInt();
		failure = Stream.of(FailureType.values()).filter(t -> t.value == type).findFirst().get();
		version = reader.readDouble();
		language = reader.readString();
		displayVersion = reader.readString();
	}

	@Override
	protected void writeEntries(PacketWriter packet) {
		RawWriter writer = new RawWriter(packet);
		writer.writeInt(failure.value);
		writer.writeDouble(version);
		writer.writeString(language);
		writer.writeString(displayVersion);
	}

}
