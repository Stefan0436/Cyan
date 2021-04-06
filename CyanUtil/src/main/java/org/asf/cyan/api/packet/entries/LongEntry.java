package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.asf.cyan.api.packet.PacketEntry;

public class LongEntry implements PacketEntry<Long> {

	private Long val;

	LongEntry() {

	}

	public LongEntry(Long value) {
		val = value;
	}

	@Override
	public long length() {
		return 8;
	}

	@Override
	public long type() {
		return 124272619l;
	}

	@Override
	public boolean isCompatible(long type) {
		return type == type();
	}

	@Override
	public Long get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(ByteBuffer.allocate(8).putLong(val).array());
	}

	@Override
	public PacketEntry<Long> importStream(InputStream source, long amount) throws IOException {
		return new LongEntry(ByteBuffer.wrap(source.readNBytes((int)amount)).getLong());
	}

}
