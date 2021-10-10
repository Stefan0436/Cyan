package org.asf.cyan.api.packet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.cyan.api.packet.entries.BooleanEntry;
import org.asf.cyan.api.packet.entries.ByteArrayEntry;
import org.asf.cyan.api.packet.entries.ByteEntry;
import org.asf.cyan.api.packet.entries.CharEntry;
import org.asf.cyan.api.packet.entries.DoubleEntry;
import org.asf.cyan.api.packet.entries.FloatEntry;
import org.asf.cyan.api.packet.entries.IntEntry;
import org.asf.cyan.api.packet.entries.LongEntry;
import org.asf.cyan.api.packet.entries.SerializingEntry;
import org.asf.cyan.api.packet.entries.StringEntry;

/**
 * 
 * Packet parsing system, parses network packets created by the
 * {@link PacketEntryWriter PacketBuilder}.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class PacketEntryReader {

	public class PkHeader {
		public byte type;
	}

	public class Entry {
		public Entry next;
		public PacketEntry<?> value;
	}

	protected Entry currentEntry;

	@SuppressWarnings("rawtypes")
	protected HashMap<Byte, Class<? extends PacketEntry>> entryTypes = new HashMap<Byte, Class<? extends PacketEntry>>();

	/**
	 * Creates a new parser instance, registers the default entry types.
	 */
	public PacketEntryReader() {
		registerType(new StringEntry(null));
		registerType(new LongEntry(0l));
		registerType(new IntEntry(0));
		registerType(new ByteEntry((byte) 0));
		registerType(new ByteArrayEntry(new byte[0]));
		registerType(new SerializingEntry<Integer>(0));
		registerType(new FloatEntry(0f));
		registerType(new DoubleEntry(0d));
		registerType(new CharEntry('\000'));
		registerType(new BooleanEntry(false));
	}

	/**
	 * Registers an entry type, defaults are already registered.
	 * 
	 * @param entry Packet entry
	 */
	public void registerType(PacketEntry<?> entry) {
		entryTypes.put(entry.type(), entry.getClass());
	}

	protected long version = 1l;

	/**
	 * Sets the version needed to parse the packet, default is one.
	 * 
	 * @param version Packet version.
	 */
	public void setSupportedVersion(long version) {
		this.version = version;
	}

	/**
	 * Imports the packet bytes and constructs the entries.
	 * 
	 * @throws IOException
	 */
	public void read(InputStream input) throws IOException {
		long ver = ByteBuffer.wrap(input.readNBytes(8)).getLong();
		if (ver != version)
			throw new IllegalArgumentException("Packet version mismatch, got: " + ver + ", expected: " + version);

		int headers = ByteBuffer.wrap(input.readNBytes(4)).getInt();
		ArrayList<PkHeader> headersLst = new ArrayList<PkHeader>();
		for (int i = 0; i < headers; i++) {
			PkHeader head = new PkHeader();
			head.type = (byte) input.read();
			headersLst.add(head);
		}

		currentEntry = new Entry();
		Entry entry = currentEntry;
		for (PkHeader header : headersLst) {
			try {
				@SuppressWarnings("rawtypes")
				Constructor<? extends PacketEntry> ctor = entryTypes.get(header.type).getDeclaredConstructor();
				ctor.setAccessible(true);
				PacketEntry<?> ent = ctor.newInstance();
				ent = ent.importStream(input);

				entry.value = ent;
			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to create packet entry with type: " + header.type, e);
			}
			entry.next = new Entry();
			entry = entry.next;
		}
	}

	/**
	 * Retrieves the next entry
	 * 
	 * @param <T> Entry type
	 * @return Entry or null if end was reached.
	 */
	@SuppressWarnings("unchecked")
	public <T> PacketEntry<T> nextEntry() {
		if (currentEntry == null)
			return null;
		else {
			PacketEntry<T> ent = (PacketEntry<T>) currentEntry.value;
			currentEntry = currentEntry.next;
			return ent;
		}
	}
}
