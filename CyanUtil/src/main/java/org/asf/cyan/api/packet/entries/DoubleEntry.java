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
	public byte type() {
		return 6;
	}

	@Override
	public boolean isCompatible(byte type) {
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
	public PacketEntry<Double> importStream(InputStream source) throws IOException {
		return new DoubleEntry(ByteBuffer.wrap(source.readNBytes(8)).getDouble());
	}

}
