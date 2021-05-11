package org.asf.cyan.api.internal.test;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.network.PacketReader;
import org.asf.cyan.api.network.channels.AbstractPacketProcessor;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;
import org.asf.cyan.api.network.channels.PacketChannel;

public class TestPacketChannel extends PacketChannel {

	public static class TestProcessor extends AbstractPacketProcessor {

		@Override
		public String id() {
			return "test";
		}

		@Override
		protected void process(PacketReader packet) {
			if (getChannel().getSide() == GameSide.SERVER) {
				getChannel().sendPacket("disconnect");
			}
		}

	}

	public static class TestClientProcessor extends ClientPacketProcessor {

		@Override
		public String id() {
			return "disconnect";
		}

		@Override
		protected void process(PacketReader packet) {
			disconnect();
		}

	}

	@Override
	protected PacketChannel newInstance() {
		return new TestPacketChannel();
	}

	@Override
	public String id() {
		return "test";
	}

	@Override
	public void setup() {
		register(TestProcessor.class);
		register(TestClientProcessor.class);
	}

}
