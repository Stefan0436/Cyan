package org.asf.cyan.api.internal.modkit.components._1_16.common.network;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.network.packets.FlowPacketBuilder;
import org.asf.cyan.api.network.OutputFlow;
import org.asf.cyan.api.network.PacketWriter;
import org.asf.cyan.api.packet.PacketEntry;

public class PacketWriterImplementation extends PacketWriter implements IModKitComponent {

	private OutputFlow flow;
	private FlowPacketBuilder builder;

	@Override
	public void initializeComponent() {
		implementation = this;
	}

	@Override
	protected PacketWriter newInstance() {
		return new PacketWriterImplementation();
	}

	@Override
	protected void init(OutputFlow flow) {
		this.flow = flow;
	}

	@Override
	public <T> PacketWriter writeEntry(PacketEntry<T> entry) {
		if (builder == null) {
			builder = new FlowPacketBuilder(flow);
		}
		builder.add(entry);
		return this;
	}

}
