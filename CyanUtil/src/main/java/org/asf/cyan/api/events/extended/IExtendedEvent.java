package org.asf.cyan.api.events.extended;

import java.util.function.Consumer;

import org.asf.cyan.api.events.core.EventBus;
import org.asf.cyan.api.events.core.IEventListener;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.TargetModloader;

/**
 * 
 * Extended Event Interface -- Keeps the event bus in memory instead of
 * searching by name. (reduces overhead)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 * @param <T> Event parameter type
 */
@TargetModloader(value = Modloader.class, any = true)
public interface IExtendedEvent<T extends EventObject> extends IModloaderComponent {
	/**
	 * Internal event bus retrieval system (do not use)
	 */
	public EventBus getBus();

	/**
	 * Retrieves the event channel name
	 * 
	 * @return Channel name (used to create the bus)
	 */
	public String channelName();

	/**
	 * Assigns the event bus that was (do not use)
	 * 
	 * @param eventBus
	 */
	public void assign(EventBus eventBus);

	/**
	 * Dispatches this event
	 * 
	 * @param parameters EventObject instance
	 */
	public default void dispatch(T parameters) {
		getBus().dispatch(parameters);
	}

	/**
	 * Attaches an event listener
	 * 
	 * @param listener Event listener
	 */
	public default void attach(IEventListener listener) {
		getBus().attachListener(listener);
	}

	/**
	 * Attaches an event listener
	 * 
	 * @param name     Event listener name
	 * @param listener Event listener
	 */
	public default void attachListener(String name, Consumer<T> listener) {
		getBus().attachListener(new IExtendedEventListener<T>() {

			@Override
			public String getListenerName() {
				return name;
			}

			@Override
			public void received(T params) {
				listener.accept(params);
			}

		});
	}

	/**
	 * Attaches a synchronized event listener
	 * 
	 * @param name     Event listener name
	 * @param listener Event listener
	 */
	public default void attachSynchronizedListener(String name, Consumer<T> listener) {
		getBus().attachListener(new IExtendedSynchronizedEventListener<T>() {

			@Override
			public String getListenerName() {
				return name;
			}

			@Override
			public void received(T params) {
				listener.accept(params);
			}

		});
	}

	/**
	 * Called after the event has been instantiated, the 'this' object should be
	 * stored for use with a getInstance() static method.
	 */
	public void afterInstantiation();

	/**
	 * Retrieves this event's instance, <b>required override.</b>
	 */
	public static IExtendedEvent<?> getInstance() {
		return null;
	}
}
