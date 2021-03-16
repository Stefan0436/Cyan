package org.asf.cyan.loader.eventbus;

import org.asf.cyan.api.events.core.EventBusFactory;

class CyanEventBusFactory extends EventBusFactory<CyanEventBus> {

	@Override
	public CyanEventBus createBus(String channel) {
		return createInstance(CyanEventBus.class).assign(channel);
	}

}
