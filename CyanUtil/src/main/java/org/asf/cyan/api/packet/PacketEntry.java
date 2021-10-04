package org.asf.cyan.api.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface PacketEntry<T> {

	public byte type();

	public void transfer(OutputStream destination) throws IOException;

	public boolean isCompatible(long type);

	public PacketEntry<T> importStream(InputStream source) throws IOException;

	public T get();
}
