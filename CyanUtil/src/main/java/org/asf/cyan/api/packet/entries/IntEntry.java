package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.asf.cyan.api.packet.PacketEntry;

public class IntEntry implements PacketEntry<Integer> {

	private Integer val;

	IntEntry() {

	}

	public IntEntry(Integer value) {
		val = value;
	}

	@Override
	public long length() {
		return 4;
	}

	@Override
	public long type() {
		return 1212632l;
	}

	@Override
	public boolean isCompatible(long type) {
		return type == type();
	}

	@Override
	public Integer get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(ByteBuffer.allocate(4).putInt(val).array());
	}

	@Override
	public PacketEntry<Integer> importStream(InputStream source, long amount) throws IOException {
		return new IntEntry(ByteBuffer.wrap(source.readNBytes((int)amount)).getInt());
	}

}
