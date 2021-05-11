package org.asf.cyan.api.internal;

import net.minecraft.client.Minecraft;

public interface ClientPacketListenerExtension {
	public String getServerBrand();

	public Minecraft cyanGetMinecraft();
}
