package org.asf.cyan.api.internal.modkit.components._1_16.common.network;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.network.packets.FlowPacketParser;
import org.asf.cyan.api.network.ByteFlow;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.packet.PacketEntry;

public class PacketReaderImplementation extends PacketReader implements IModKitComponent {

	private ByteFlow flow;
	private FlowPacketParser parser = null;

	@Override
	public void initializeComponent() {
		implementation = this;
	}

	@Override
	protected PacketReader newInstance() {
		return new PacketReaderImplementation();
	}

	@Override
	protected void init(ByteFlow flow) {
		this.flow = flow;
	}

	@Override
	public <T> PacketEntry<T> readEntry() {
		if (parser == null) {
			parser = new FlowPacketParser(flow);
		}
		return parser.nextEntry();
	}

}
