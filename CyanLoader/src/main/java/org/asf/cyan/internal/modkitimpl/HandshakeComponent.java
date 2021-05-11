package org.asf.cyan.internal.modkitimpl;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.events.network.EarlyCyanClientHandshakeEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.internal.modkitimpl.handshake.CyanHandshakePacketChannel;
import org.asf.cyan.internal.modkitimpl.handshake.packets.HandshakeProtocolPacket;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;
import org.asf.cyan.internal.modkitimpl.util.ScreenUtil;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

@CYAN_COMPONENT
public class HandshakeComponent implements IEventListenerContainer {

	protected static void initComponent() {
		BaseEventController.addEventContainer(new HandshakeComponent());
	}

	@SimpleEvent(value = EarlyCyanClientHandshakeEvent.class, synchronize = true)
	public void handshakeStartClient(ClientConnectionEventObject event) throws InterruptedException {
		ScreenUtil.getImpl().setScreenToTitle(event);
		CyanHandshakePacketChannel channel = HandshakeUtils.getImpl().getChannel(CyanHandshakePacketChannel.class,
				event);
		HandshakeProtocolPacket packet = new HandshakeProtocolPacket();
		packet.protocolVersion = Protocols.MODKIT_PROTOCOL;
		packet.write(channel);
	}

}
