package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.asf.cyan.api.packet.PacketEntry;

public class DoubleEntry implements PacketEntry<Double> {

	private Double val;

	DoubleEntry() {

	}

	public DoubleEntry(Double value) {
		val = value;
	}

	@Override
	public long length() {
		return 8;
	}

	@Override
	public long type() {
		return 1162733142417l;
	}

	@Override
	public boolean isCompatible(long type) {
		return type == type();
	}

	@Override
	public Double get() {
		return val;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		destination.write(ByteBuffer.allocate(8).putDouble(val).array());
	}

	@Override
	public PacketEntry<Double> importStream(InputStream source, long amount) throws IOException {
		return new DoubleEntry(ByteBuffer.wrap(source.readNBytes((int)amount)).getDouble());
	}

}
