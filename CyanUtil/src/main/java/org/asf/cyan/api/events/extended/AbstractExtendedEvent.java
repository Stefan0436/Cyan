package org.asf.cyan.api.events.extended;

import java.lang.reflect.InvocationTargetException;

import org.asf.cyan.api.events.core.EventBus;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.TargetModloader;

/**
 * 
 * Abstract extended event
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 * @param <T> Event parameter type
 * 
 */
@TargetModloader(value = Modloader.class, any = true)
public abstract class AbstractExtendedEvent<T extends EventObject> implements IExtendedEvent<T> {

	private EventBus bus;

	@Override
	public EventBus getBus() {
		return bus;
	}

	@Override
	public void assign(EventBus eventBus) {
		bus = eventBus;
		try {
			if (getClass().getMethod("getInstance").invoke(null) == null) {
				throw new RuntimeException(
						"Extended event " + getClass().getTypeName() + " has no getInstance implementation.");
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(
					"Extended event " + getClass().getTypeName() + " has no getInstance implementation.");
		}
	}

	/**
	 * Retrieves this event's instance, <b>required override.</b>
	 */
	public static AbstractExtendedEvent<?> getInstance() {
		return null;
	}

}
