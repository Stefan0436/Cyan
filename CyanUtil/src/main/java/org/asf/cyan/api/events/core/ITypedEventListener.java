package org.asf.cyan.api.events.core;

/**
 * 
 * Modloader Event Listener with a type argument
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface ITypedEventListener<T> extends IEventListener {
	public String getListenerName();

	@SuppressWarnings("unchecked")
	public default void received(Object... params) {
		try {
			if (params.length == 1) {
				received((T) params[0]);
			}
		} catch (ClassCastException e) {
		}
	}

	public void received(T param);
}
