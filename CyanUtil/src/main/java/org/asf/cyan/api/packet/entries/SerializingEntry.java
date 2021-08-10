package org.asf.cyan.api.packet.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

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
	public long length() {
		CounterOutputStream strm = new CounterOutputStream();
		try {
			serialize(val, strm);
			strm.close();
		} catch (IOException e) {
		}
		return strm.length();
	}

	@Override
	public long type() {
		return 1271422171532l;
	}

	@Override
	public boolean isCompatible(long type) {
		return type == type();
	}

	@Override
	public T get() {
		if (failure)
			throw new IllegalArgumentException("Deserialization failure, cannot get serialized entry.");
		return val;
	}

	void serialize(Object obj, OutputStream output) throws IOException {
		ObjectOutputStream serializer = new ObjectOutputStream(output);
		serializer.writeObject(obj);
	}

	Object deserialize(InputStream input) throws IOException {
		ObjectInputStream deserializer = new ObjectInputStream(input);
		Object obj;
		try {
			obj = deserializer.readObject();
		} catch (ClassNotFoundException | IOException e) {
			throw new IOException(e);
		}
		return obj;
	}

	@Override
	public void transfer(OutputStream destination) throws IOException {
		serialize(val, destination);
	}

	@Override
	@SuppressWarnings("unchecked")
	public PacketEntry<T> importStream(InputStream source, long amount) throws IOException {
		try {
			return new SerializingEntry<T>((T)deserialize(source));
		} catch (IOException e) {
			return new SerializingEntry<T>(true);
		}
	}

}
