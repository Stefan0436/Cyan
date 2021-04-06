package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.asf.cyan.api.packet.PacketEntry;

public class ByteArrayEntry implements PacketEntry<byte[]> {

	private byte[] val;

	ByteArrayEntry() {

	}

	public ByteArrayEntry(byte[] value) {
		val = value;
	}

	@Override
	public long length() {
		return val.length;
	}

	@Override
	public long type() {
		return 1143732171330301337l;
	}

	@Override
	public boolean isCompatible(long type) {
		return type == type();
	}

	@Override
	public byte[] get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(val);
	}

	@Override
	public PacketEntry<byte[]> importStream(InputStream source, long amount) throws IOException {
		return new ByteArrayEntry(source.readNBytes((int)amount));
	}

}
