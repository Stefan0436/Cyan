package org.asf.cyan.api.events.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

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
	 * Checks if the bus has no listeners
	 */
	public abstract boolean isEmpty();

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
	 * @return Overall event completion status
	 */
	public abstract ResultContainer dispatch(Object... params);

	/**
	 * Called to execute the event.
	 * 
	 * @param completionHook Called on event completion
	 * @param params         Event parameters.
	 * @return Overall event completion status
	 */
	public abstract void dispatch(Consumer<ResultContainer> completionHook, Object[] params);

	/**
	 * Call a listener
	 * 
	 * @param listener       The listener to call
	 * @param params         Event parameters
	 * @param completionHook Hook to call on event completion
	 */
	protected ResultContainer callListener(IEventListener listener, Object[] params,
			Consumer<ResultContainer> completionHook) {
		ResultContainer container = new ResultContainer(this);
		debug("Dispatching event " + getChannel() + " to listener " + listener.getListenerName() + "...");
		try {
			container.completed = false;
			if (listener instanceof ISynchronizedEventListener) {
				listener.received(params);
				container.completed = true;
				completionHook.accept(container);
			} else {
				Thread listenerThread = new Thread(() -> {
					try {
						listener.received(params);
						container.completed = true;
						completionHook.accept(container);
					} catch (RuntimeException e) {
						container.completed = true;
						completionHook.accept(container);
						throw e;
					}
				}, listener.getListenerName() + " Event Listener");
				listenerThread.start();
			}
		} catch (Exception e) {
			container.completed = true;
			container.error = true;
			completionHook.accept(container);
			if (e instanceof RuntimeException)
				throw e;
			else
				error("Exception caught in event listener " + listener.getListenerName(), e);
		}
		return container;
	}

	protected void setResult(ResultContainer container, boolean completed, boolean error) {
		if (container.bus == this) {
			container.completed = completed;
			container.error = error;
		}
	}

	/**
	 * Class containing information about event execution, completed will always
	 * become true when execution completes. The error field will show success or
	 * error.
	 */
	public class ResultContainer {
		protected EventBus bus;

		public ResultContainer(EventBus owner) {
			this.bus = owner;
		}

		protected boolean completed = false;
		protected boolean error = false;

		public boolean hasCompleted() {
			return completed;
		}

		public boolean hasError() {
			return error;
		}
	}
}
