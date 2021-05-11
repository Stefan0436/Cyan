package org.asf.cyan.api.internal.modkit.components._1_16.client;

import org.asf.cyan.api.events.network.EarlyCyanClientHandshakeEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.modkit.handshake.CyanHandshakePacketChannel;
import org.asf.cyan.api.internal.modkit.handshake.packets.HandshakeProtocolPacketProcessor;
import org.asf.cyan.api.internal.modkit.handshake.packets.content.HandshakeProtocolPacket;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import net.minecraft.client.gui.screens.ReceivingLevelScreen;

public class HandshakeComponent implements IModKitComponent, IEventListenerContainer {

	@Override
	public void initializeComponent() {
		BaseEventController.addEventContainer(this);
	}

	@SimpleEvent(value = EarlyCyanClientHandshakeEvent.class, synchronize = true)
	public void handshakeStartClient(ClientConnectionEventObject event) throws InterruptedException {
		event.getClient().setScreen(new ReceivingLevelScreen());
		CyanHandshakePacketChannel channel = CyanHandshakePacketChannel.getChannel(CyanHandshakePacketChannel.class,
				event);
		HandshakeProtocolPacket packet = new HandshakeProtocolPacket();
		packet.protocolVersion = HandshakeProtocolPacketProcessor.PROTOCOL;
		packet.write(channel);
	}

}
