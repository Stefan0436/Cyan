package org.asf.cyan.loader.eventbus;

import org.asf.cyan.api.events.core.EventBus;
import org.asf.cyan.api.events.core.IEventListener;

class CyanEventBus extends EventBus {

	private String channel;
	private CyanEventList listeners = new CyanEventList();

	@Override
	public String getChannel() {
		return channel;
	}

	@Override
	public void attachListener(IEventListener listener) {
		if (listeners.contains(listener)) {
			throw new IllegalArgumentException(
					"Listener conflict; listener name: " + listener.getListenerName() + ", channel: " + getChannel());
		}
		listeners.add(listener);
	}

	@Override
	public void dispatch(Object... params) {
		for (IEventListener listener : listeners) {
			callListener(listener, params);
		}
	}

	CyanEventBus assign(String channel) {
		this.channel = channel;
		return this;
	}

	protected static CyanEventBus getNewInstance() {
		return new CyanEventBus();
	}

}
