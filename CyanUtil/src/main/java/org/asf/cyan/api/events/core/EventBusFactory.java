package org.asf.cyan.api.events.core;

/**
 * 
 * Event bus factory, system to create event busses.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class EventBusFactory<T extends EventBus> {
	private EventBus root;

	/**
	 * Assigns the root event bus.
	 * 
	 * @param root Root event bus.
	 */
	void assignRoot(EventBus root) {
		if (this.root != null)
			throw new IllegalStateException("Root event bus already assigned!");

		this.root = root;
	}

	/**
	 * Creates a new event bus
	 * @param channel Event bus channel
	 * @return New event bus.
	 */
	public abstract T createBus(String channel);

	/**
	 * Creates a new EventBus
	 * 
	 * @param cls Event bus class
	 * @return New instane of the event bus.
	 */
	protected T createInstance(Class<T> cls) {
		return EventBus.instanciate(this, getRoot(), cls);
	}

	/**
	 * Gets the root event bus.
	 */
	protected EventBus getRoot() {
		return root;
	}

	/**
	 * Gets the child bus of the specified bus.
	 */
	public EventBus getChildBus(EventBus bus) {
		return bus.getChildBus();
	}
}
