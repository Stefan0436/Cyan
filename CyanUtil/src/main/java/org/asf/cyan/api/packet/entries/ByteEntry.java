package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.asf.cyan.api.packet.PacketEntry;

public class ByteEntry implements PacketEntry<Byte> {

	private Byte val;

	ByteEntry() {

	}

	public ByteEntry(Byte value) {
		val = value;
	}

	@Override
	public long length() {
		return 1;
	}

	@Override
	public long type() {
		return 114373217l;
	}

	@Override
	public boolean isCompatible(long type) {
		return type == type();
	}

	@Override
	public Byte get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(val);
	}

	@Override
	public PacketEntry<Byte> importStream(InputStream source, long amount) throws IOException {
		return new ByteEntry((byte)source.read());
	}

}
