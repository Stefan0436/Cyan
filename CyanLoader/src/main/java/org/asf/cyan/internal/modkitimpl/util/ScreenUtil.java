package org.asf.cyan.internal.modkitimpl.util;

import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.network.channels.ClientPacketProcessor;

public abstract class ScreenUtil {

	protected static ScreenUtil impl;

	public static ScreenUtil getImpl() {
		return impl;
	}

	public abstract void setScreenToReceiveLevel(ClientConnectionEventObject event);

	public abstract void setScreenToReceiveLevel(ClientPacketProcessor processor);

	public abstract void setScreenToWorld(ClientPacketProcessor processor);

}
