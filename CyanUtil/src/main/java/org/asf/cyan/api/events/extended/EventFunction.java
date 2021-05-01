package org.asf.cyan.api.events.extended;

import org.asf.cyan.api.events.core.EventBus;
import org.asf.cyan.api.events.extended.EventObject.EventResult;

class EventFunction extends AsyncFunction<EventObject.EventResult> {

	private EventBus bus;
	private EventObject obj;

	public EventFunction(EventBus bus, EventObject obj) {
		this.bus = bus;
		this.obj = obj;
	}

	@Override
	protected boolean async() {
		return false;
	}

	@Override
	protected void run() {
		bus.dispatch((result) -> {
			if (obj.getResult() == EventResult.UNDEFINED)
				obj.setResult(EventResult.CONTINUE);
			setResult(obj.getResult());
		}, new Object[] { obj });
	}

}
