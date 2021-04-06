package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.asf.cyan.api.packet.PacketEntry;

public class FloatEntry implements PacketEntry<Float> {

	private Float val;

	FloatEntry() {

	}

	public FloatEntry(Float value) {
		val = value;
	}

	@Override
	public long length() {
		return 4;
	}

	@Override
	public long type() {
		return 11824271332l;
	}

	@Override
	public boolean isCompatible(long type) {
		return type == type();
	}

	@Override
	public Float get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(ByteBuffer.allocate(4).putFloat(val).array());
	}

	@Override
	public PacketEntry<Float> importStream(InputStream source, long amount) throws IOException {
		return new FloatEntry(ByteBuffer.wrap(source.readNBytes((int)amount)).getFloat());
	}

}
