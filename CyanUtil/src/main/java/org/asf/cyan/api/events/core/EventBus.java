package org.asf.cyan.api.events.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.asf.cyan.api.common.CyanComponent;

/**
 * 
 * Event busses, modloader event system.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class EventBus extends CyanComponent {

	private EventBus childBus;

	protected EventBus() {
	}

	/**
	 * Get the child bus (if any)
	 */
	protected EventBus getChildBus() {
		return childBus;
	}

	@SuppressWarnings("unchecked")
	static <T extends EventBus> T instanciate(EventBusFactory<T> factory, EventBus root, Class<T> cls) {
		T bus;
		try {
			bus = cls.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			try {
				Method meth = cls.getDeclaredMethod("getNewInstance");
				if (!Modifier.isStatic(meth.getModifiers()))
					throw new RuntimeException(e);

				meth.setAccessible(true);
				bus = (T) meth.invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e2) {
				throw new RuntimeException(e);
			}
		}

		if (factory.getRoot() == null) {
			factory.assignRoot(bus);
			return bus;
		}

		EventBus parent = root;
		while (parent.getChildBus() != null) {
			parent = parent.getChildBus();
		}

		parent.childBus = bus;

		return bus;
	}

	/**
	 * Gets the channel of the event bus.
	 */
	public abstract String getChannel();

	/**
	 * Attaches event listeners to the event bus.
	 */
	public abstract void attachListener(IEventListener listener);

	/**
	 * Called to execute the event.
	 * 
	 * @param params Event parameters.
	 */
	public abstract void dispatch(Object... params);

	/**
	 * Call a listener
	 * 
	 * @param listener The listener to call
	 */
	protected void callListener(IEventListener listener, Object[] params) {
		debug("Dispatching event " + getChannel() + " to listener " + listener.getListenerName() + "...");
		try {
			if (listener instanceof ISynchronizedEventListener) {
				listener.received(params);
			} else {
				Thread listenerThread = new Thread(() -> listener.received(params),
						listener.getListenerName() + " Event Listener");
				listenerThread.start();
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw e;
			else
				error("Exception caught in event listener " + listener.getListenerName(), e);
		}
	}
}
