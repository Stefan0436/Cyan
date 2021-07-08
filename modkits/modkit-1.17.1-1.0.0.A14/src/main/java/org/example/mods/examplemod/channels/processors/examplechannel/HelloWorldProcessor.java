package org.example.mods.examplemod.channels.processors.examplechannel;

import modkit.network.PacketReader;
import modkit.network.channels.AbstractPacketProcessor;

import org.example.mods.examplemod.channels.packets.examplechannel.HelloWorldPacket;

public class HelloWorldProcessor extends AbstractPacketProcessor {

	@Override
	public String id() {
		return "hello.world"; // The packet id
	}

	@Override
	protected void process(PacketReader packet) {
		// Read the packet into a wrapper class:
		HelloWorldPacket pkg = new HelloWorldPacket().read(packet);
		
		// Print the message to the console:
		info("Hi " + pkg.playerName + ", the server says " + pkg.message);
	}

}
