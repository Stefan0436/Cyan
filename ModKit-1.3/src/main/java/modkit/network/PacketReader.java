package modkit.network;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.api.packet.entries.ByteArrayEntry;
import org.asf.cyan.api.packet.entries.CharEntry;
import org.asf.cyan.api.packet.entries.DoubleEntry;
import org.asf.cyan.api.packet.entries.FloatEntry;
import org.asf.cyan.api.packet.entries.IntEntry;
import org.asf.cyan.api.packet.entries.LongEntry;
import org.asf.cyan.api.packet.entries.SerializingEntry;
import org.asf.cyan.api.packet.entries.StringEntry;
import org.asf.cyan.api.versioning.Version;

/**
 * 
 * Packet Content Reader
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class PacketReader {

	public static class RawReader {
		public PacketReader reader;

		public RawReader(PacketReader reader) {
			this.reader = reader;
		}

		public byte[] readNBytes(int num) {
			return reader.readNBytes(num);
		}

		public byte[] readAllBytes() {
			return reader.readAllBytes();
		}

		/**
		 * @deprecated Incorrect return type, use readSingleByte() instead
		 */
		@Deprecated
		public int readByte() {
			return reader.readByte();
		}

		public byte readSingleByte() {
			return reader.readByte();
		}

		public String readString() {
			return new String(readNBytes(readInt()));
		}

		public char readChar() {
			return ByteBuffer.wrap(readNBytes(2)).getChar();
		}

		public int readInt() {
			return ByteBuffer.wrap(readNBytes(4)).getInt();
		}

		public boolean readBoolean() {
			int data = readByte();
			if (data != 0)
				return true;
			else
				return false;
		}

		public long readLong() {
			return ByteBuffer.wrap(readNBytes(8)).getLong();
		}

		public short readShort() {
			return ByteBuffer.wrap(readNBytes(2)).getShort();
		}

		public float readFloat() {
			return ByteBuffer.wrap(readNBytes(4)).getFloat();
		}

		public double readDouble() {
			return ByteBuffer.wrap(readNBytes(8)).getDouble();
		}

		public PacketReader getReader() {
			return reader;
		}

		public String readVarString() {
			return new String(readNBytes(readVarInt()));
		}

		public UUID readUUID() {
			return new UUID(readLong(), readLong());
		}

		public int readVarInt() {
			return readVarInt(reader);
		}

		public long readVarLong() {
			return readVarLong(reader);
		}

		@SuppressWarnings("unchecked")
		public <T> T[] readArray(Class<T> type) {
			int length = readInt();
			Object[] array = (Object[]) Array.newInstance(type, length);

			for (int i = 0; i < length; i++) {
				if (type.isArray()) {
					array[i] = (T) readArray(type.getComponentType());
				} else {
					if (Byte.class.isAssignableFrom(type)) {
						array[i] = (byte) readByte();
					} else if (Integer.class.isAssignableFrom(type)) {
						array[i] = readInt();
					} else if (String.class.isAssignableFrom(type)) {
						array[i] = readString();
					} else if (Short.class.isAssignableFrom(type)) {
						array[i] = readShort();
					} else if (Long.class.isAssignableFrom(type)) {
						array[i] = readLong();
					} else if (Float.class.isAssignableFrom(type)) {
						array[i] = readFloat();
					} else if (Double.class.isAssignableFrom(type)) {
						array[i] = readDouble();
					} else if (Boolean.class.isAssignableFrom(type)) {
						array[i] = readBoolean();
					}
				}
			}

			return (T[]) array;
		}

		// Source: https://wiki.vg/Protocol
		private static int readVarInt(PacketReader reader) {
			int numRead = 0;
			int result = 0;
			byte read;
			do {
				read = (byte) reader.readRawByte();
				int value = (read & 0b01111111);
				result |= (value << (7 * numRead));

				numRead++;
				if (numRead > 5) {
					throw new RuntimeException("VarInt is too big");
				}
			} while ((read & 0b10000000) != 0);

			return result;
		}

		private static long readVarLong(PacketReader reader) {
			long numRead = 0;
			long result = 0;
			byte read;
			do {
				read = (byte) reader.readRawByte();
				long value = (read & 0b01111111);
				result |= (value << (7 * numRead));

				numRead++;
				if (numRead > 10) {
					throw new RuntimeException("VarLong is too big");
				}
			} while ((read & 0b10000000) != 0);

			return result;
		}
	}

	private ByteFlow flow;

	protected int readByteInternal() {
		return flow.read();
	}

	protected static PacketReader implementation;

	protected abstract PacketReader newInstance();

	protected abstract void init(ByteFlow flow);

	/**
	 * Reads the next byte
	 * 
	 * @return Byte
	 */
	public byte readByte() {
		return flow.read();
	}

	/**
	 * Reads the next byte
	 * 
	 * @return Byte
	 * @deprecated Incorrect return type, readByte() instead
	 */
	public int readRawByte() {
		return readByte();
	}

	/**
	 * Reads the given amount of bytes
	 * 
	 * @param count Byte count
	 * @return Byte array
	 */
	public byte[] readNBytes(int count) {
		byte[] buffer = new byte[count];
		for (int i = 0; i < count; i++) {
			if (!flow.hasNext())
				break;
			int b = readRawByte();
			buffer[i] = (byte) b;
		}
		return buffer;
	}

	/**
	 * Reads all available bytes (stops if size reaches integer max or if the end of
	 * the byte flow is reached)
	 * 
	 * @return Byte array
	 */
	public byte[] readAllBytes() {
		ArrayList<Byte> bytes = new ArrayList<Byte>();
		long count = 0;
		while (true) {
			if (count + 1l > Integer.MAX_VALUE)
				break;
			if (!flow.hasNext())
				break;
			bytes.add(readByte());
			count++;
		}

		int i = 0;
		byte[] buffer = new byte[bytes.size()];
		for (Byte b : bytes)
			buffer[i++] = b;

		return buffer;
	}

	/**
	 * Creates a new packet reader
	 * 
	 * @param flow Input byte flow
	 */
	public static PacketReader create(ByteFlow flow) {
		PacketReader reader = implementation.newInstance();
		reader.flow = flow;
		reader.init(flow);
		return reader;
	}

	/**
	 * Reads the next entry
	 * 
	 * @param <T> Entry type
	 * @return Packet entry
	 */
	public abstract <T> PacketEntry<T> readEntry();

	/**
	 * Reads the next String entry
	 * 
	 * @return String value or null
	 */
	public String readString() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof StringEntry)
			return entry.get().toString();
		else
			return null;
	}

	/**
	 * Reads the next integer entry
	 * 
	 * @return Integer value or -1
	 */
	public int readInt() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof IntEntry)
			return (int) entry.get();
		else
			return -1;
	}

	/**
	 * Reads the next long entry
	 * 
	 * @return Long value or -1
	 */
	public long readLong() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof LongEntry)
			return (long) entry.get();
		else
			return -1;
	}

	/**
	 * Reads the next floating-point entry
	 * 
	 * @return Float value or -1
	 */
	public float readFloat() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof FloatEntry)
			return (float) entry.get();
		else
			return -1;
	}

	/**
	 * Reads the next double-precision floating-point entry
	 * 
	 * @return Double value or -1
	 */
	public double readDouble() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof DoubleEntry)
			return (double) entry.get();
		else
			return -1;
	}

	/**
	 * Reads the next character entry (not recommended, use strings instead)
	 * 
	 * @return Character value or -1
	 */
	public char readChar() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof CharEntry)
			return (char) entry.get();
		else
			return (char) -1;
	}

	/**
	 * Reads the next byte array entry
	 * 
	 * @return Character value or null
	 */
	public byte[] readBytes() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof ByteArrayEntry)
			return (byte[]) entry.get();
		else
			return null;
	}

	/**
	 * Reads the next boolean
	 * 
	 * @return Boolean value or false
	 */
	public boolean readBoolean() {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof SerializingEntry) {
			Object obj = entry.get();
			if (obj instanceof Boolean)
				return (boolean) obj;
			else
				return false;
		} else
			return false;
	}

	/**
	 * Reads the next object value
	 * 
	 * @param <T>  Value type
	 * @param type Value class
	 * @return Value or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T readObject(Class<T> type) {
		PacketEntry<?> entry = readEntry();
		if (entry instanceof SerializingEntry) {
			Object obj = entry.get();
			if (type.isAssignableFrom(obj.getClass()))
				return (T) obj;
			else
				return null;
		} else
			return null;
	}

	/**
	 * Reads the next Version value
	 * 
	 * @return Value or null
	 */
	public Version readVersion() {
		String str = readString();
		if (str == null)
			return null;
		return Version.fromString(str);
	}

	/**
	 * Reads the next array value
	 * 
	 * @param <T>  Component type
	 * @param type Component type class
	 * @return Array instance
	 */
	@SuppressWarnings("unchecked")
	public <T> T[] readArray(Class<T> type) {
		int length = readInt();
		Object[] array = (Object[]) Array.newInstance(type, length);

		for (int i = 0; i < length; i++) {
			if (type.isArray()) {
				array[i] = (T) readArray(type.getComponentType());
			} else {
				if (Byte.class.isAssignableFrom(type)) {
					array[i] = (byte) readRawByte();
				} else if (Integer.class.isAssignableFrom(type)) {
					array[i] = readInt();
				} else if (String.class.isAssignableFrom(type)) {
					array[i] = readString();
				} else if (Short.class.isAssignableFrom(type)) {
					array[i] = (short) readInt();
				} else if (Long.class.isAssignableFrom(type)) {
					array[i] = readLong();
				} else if (Float.class.isAssignableFrom(type)) {
					array[i] = readFloat();
				} else if (Double.class.isAssignableFrom(type)) {
					array[i] = readDouble();
				} else if (Boolean.class.isAssignableFrom(type)) {
					array[i] = readBoolean();
				} else {
					array[i] = readObject(type);
				}
			}
		}

		return (T[]) array;
	}

}
