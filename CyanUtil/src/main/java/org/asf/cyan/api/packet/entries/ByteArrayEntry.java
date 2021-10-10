package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.asf.cyan.api.packet.PacketEntry;

public class ByteArrayEntry implements PacketEntry<byte[]> {

	private byte[] val;

	ByteArrayEntry() {

	}

	public ByteArrayEntry(byte[] value) {
		val = value;
	}

	@Override
	public byte type() {
		return 1;
	}

	@Override
	public boolean isCompatible(byte type) {
		return type == type();
	}

	@Override
	public byte[] get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(ByteBuffer.allocate(4).putInt(val.length).array());
		destination.write(val);
	}

	@Override
	public PacketEntry<byte[]> importStream(InputStream source) throws IOException {
		return new ByteArrayEntry(source.readNBytes(ByteBuffer.wrap(source.readNBytes(4)).getInt()));
	}

}
