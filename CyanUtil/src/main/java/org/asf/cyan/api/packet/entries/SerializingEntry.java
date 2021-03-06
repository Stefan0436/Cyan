package org.asf.cyan.api.packet.entries;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.asf.cyan.api.packet.PacketEntry;

public class SerializingEntry<T> implements PacketEntry<T> {

	public class CounterOutputStream extends OutputStream {

		private long l = 0;

		@Override
		public void write(int arg0) throws IOException {
			l++;
		}

		public long length() {
			return l;
		}

	}

	private T val;
	private boolean failure = false;

	public SerializingEntry() {

	}

	public SerializingEntry(boolean fail) {
		failure = fail;
		val = null;
	}

	public SerializingEntry(T value) {
		val = value;
	}

	@Override
	public byte type() {
		return 7;
	}

	@Override
	public boolean isCompatible(byte type) {
		return type == type();
	}

	@Override
	public T get() {
		if (failure)
			throw new IllegalArgumentException("Deserialization failure, cannot get serialized entry.");
		return val;
	}

	void serialize(Object obj, OutputStream output) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		ObjectOutputStream serializer = new ObjectOutputStream(buffer);
		serializer.writeObject(obj);
		byte[] val = buffer.toByteArray();
		output.write(ByteBuffer.allocate(4).putInt(val.length).array());
		output.write(val);
		buffer.close();
	}

	Object deserialize(InputStream input) throws IOException {
		ByteArrayInputStream buffer = new ByteArrayInputStream(
				input.readNBytes(ByteBuffer.wrap(input.readNBytes(4)).getInt()));
		ObjectInputStream deserializer = new ObjectInputStream(buffer);
		Object obj;
		try {
			obj = deserializer.readObject();
		} catch (ClassNotFoundException | IOException e) {
			throw new IOException(e);
		}
		buffer.close();
		return obj;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		serialize(val, destination);
	}

	@Override
	@SuppressWarnings("unchecked")
	public PacketEntry<T> importStream(InputStream source) throws IOException {
		try {
			return new SerializingEntry<T>((T) deserialize(source));
		} catch (IOException e) {
			return new SerializingEntry<T>(true);
		}
	}

}
