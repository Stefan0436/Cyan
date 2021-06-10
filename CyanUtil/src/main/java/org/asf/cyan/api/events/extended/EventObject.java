package org.asf.cyan.api.events.extended;

import org.asf.cyan.api.common.CyanComponent;

public abstract class EventObject extends CyanComponent {
	public enum EventResult {
		CANCEL, CONTINUE, UNDEFINED
	}

	private EventResult result = EventResult.UNDEFINED;

	public void cancel() {
		setResult(EventResult.CANCEL);
	}

	public void setResult(EventResult result) {
		if (this.result != EventResult.CANCEL)
			this.result = result;
	}

	public EventResult getResult() {
		return result;
	}
}
