package org.example.mods.examplemod.channels;

import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.mods.AbstractMod;
import org.example.mods.examplemod.ExampleMod;
import org.example.mods.examplemod.channels.processors.examplechannel.HelloWorldProcessor;

public class ExamplePacketChannel extends PacketChannel {

	@Override
	public String id() {
		// Create our channel id
		return createChannelId("examplechannel.one");
	}

	private String createChannelId(String id) {
		// Uses our mod id, but replaces ':' with '.'
		return AbstractMod.getInstance(ExampleMod.class).getManifest().id().replace(":", ".") + "." + id;
	}

	@Override
	public void setup() {
		// Sets up packet processors
		this.register(HelloWorldProcessor.class);
	}

	@Override
	protected PacketChannel newInstance() {
		return new ExamplePacketChannel();
	}

}
