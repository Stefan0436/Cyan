package modkit.network.channels;

import org.asf.cyan.api.common.CyanComponent;

import modkit.network.ByteFlow;
import modkit.network.PacketReader;

/**
 * 
 * Abstract Packet Processor
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class AbstractPacketProcessor extends CyanComponent {

	/**
	 * Packet id this processor accepts
	 */
	public abstract String id();

	/**
	 * True to use regex matching for the packet id check
	 */
	public boolean regexId() {
		return false;
	}

	/**
	 * Prepares to process the packet
	 * 
	 * @param channel Packet channel *
	 * @return True if accepted, false otherwise
	 */
	protected boolean prepare(PacketChannel channel) {
		return true;
	}

	/**
	 * Retrieves the packet reader
	 */
	protected PacketReader getReader() {
		return reader;
	}

	/**
	 * Retrieves the packet channel
	 */
	public PacketChannel getChannel() {
		return channel;
	}

	/**
	 * Retrieves the actual packet id
	 */
	protected String getId() {
		return packetId;
	}

	/**
	 * Processes the packet
	 */
	protected abstract void process(PacketReader packet);

	private PacketReader reader;
	private PacketChannel channel;
	private String packetId;

	boolean run(PacketChannel channel, String id, ByteFlow flow) {
		this.channel = channel;
		this.packetId = id;

		if (!prepare(channel))
			return false;

		this.reader = PacketReader.create(flow);
		process(reader);
		return true;
	}

}
