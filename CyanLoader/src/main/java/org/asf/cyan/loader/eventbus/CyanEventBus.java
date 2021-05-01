package org.asf.cyan.loader.eventbus;

import java.util.function.Consumer;

import org.asf.cyan.api.events.core.EventBus;
import org.asf.cyan.api.events.core.IEventListener;
import org.asf.cyan.loader.eventbus.CyanEventList.CELEntry;

class CyanEventBus extends EventBus {

	private String channel;
	private CyanEventList listeners = new CyanEventList();
	private boolean empty = true;

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
		empty = false;
	}

	@Override
	public ResultContainer dispatch(Object... params) {
		ResultContainer result = new ResultContainer(this);
		dispatchInternal(params, result, null);
		return result;
	}

	@Override
	public void dispatch(Consumer<ResultContainer> completionHook, Object[] params) {
		ResultContainer result = new ResultContainer(this);
		dispatchInternal(params, result, completionHook);
	}

	private void dispatchInternal(Object[] params, ResultContainer result, Consumer<ResultContainer> completionHook) {
		if (isEmpty()) {
			setResult(result, true, false);
			if (completionHook != null)
				completionHook.accept(result);
		}

		for (CELEntry entry : listeners) {
			IEventListener listener = entry.listener;
			entry.result = callListener(listener, params, (container) -> {
				entry.result = container;
				if (container.hasError()) {
					setResult(result, result.hasCompleted(), true);
				}

				boolean complete = false;
				for (CELEntry ent : listeners) {
					boolean completed = false;
					if (ent.result != null) {
						completed = ent.result.hasCompleted();
					}
					if (completed) {
						complete = true;
						break;
					}
				}

				if (complete) {
					setResult(result, true, result.hasError());
					if (completionHook != null)
						completionHook.accept(result);
				}
			});
		}
	}

	CyanEventBus assign(String channel) {
		this.channel = channel;
		return this;
	}

	protected static CyanEventBus getNewInstance() {
		return new CyanEventBus();
	}

	@Override
	public boolean isEmpty() {
		return empty;
	}

}
