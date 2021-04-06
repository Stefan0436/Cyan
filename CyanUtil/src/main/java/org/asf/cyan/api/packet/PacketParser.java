package org.asf.cyan.api.packet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

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
 * {@link PacketBuilder PacketBuilder}.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class PacketParser {

	public class PkHeader {
		public long type;
		public long length;
	}

	protected ArrayList<PacketEntry<?>> entries = new ArrayList<PacketEntry<?>>();

	@SuppressWarnings("rawtypes")
	protected HashMap<Long, Class<? extends PacketEntry>> entryTypes = new HashMap<Long, Class<? extends PacketEntry>>();

	public PacketEntry<?>[] getEntries() {
		return entries.toArray(t -> new PacketEntry<?>[t]);
	}

	/**
	 * Creates a new parser instance, registers the default entry types.
	 */
	public PacketParser() {
		registerType(new StringEntry(null));
		registerType(new LongEntry(0l));
		registerType(new IntEntry(0));
		registerType(new ByteEntry((byte) 0));
		registerType(new ByteArrayEntry(new byte[0]));
		registerType(new SerializingEntry<Integer>(0));
		registerType(new FloatEntry(0f));
		registerType(new DoubleEntry(0d));
		registerType(new CharEntry('\000'));
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
	public void importStream(InputStream input) throws IOException {
		long ver = ByteBuffer.wrap(input.readNBytes(8)).getLong();
		if (ver != version)
			throw new IllegalArgumentException("Packet version mismatch, got: " + ver + ", expected: " + version);

		int headers = ByteBuffer.wrap(input.readNBytes(4)).getInt();
		ArrayList<PkHeader> headersLst = new ArrayList<PkHeader>();
		for (int i = 0; i < headers; i++) {
			long type = ByteBuffer.wrap(input.readNBytes(8)).getLong();
			long length = ByteBuffer.wrap(input.readNBytes(8)).getLong();

			PkHeader head = new PkHeader();
			head.type = type;
			head.length = length;
			headersLst.add(head);
		}

		entries.clear();
		for (PkHeader header : headersLst) {
			try {
				@SuppressWarnings("rawtypes")
				Constructor<? extends PacketEntry> ctor = entryTypes.get(header.type).getDeclaredConstructor();
				ctor.setAccessible(true);
				PacketEntry<?> ent = ctor.newInstance();
				ent = ent.importStream(input, header.length);

				entries.add(ent);
			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to create packet entry with type: " + header.type, e);
			}
		}
	}
}
