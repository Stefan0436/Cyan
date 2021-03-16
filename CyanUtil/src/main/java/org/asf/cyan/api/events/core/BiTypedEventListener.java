package org.asf.cyan.api.events.core;

/**
 * 
 * Modloader Event Listener with two type arguments
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * 
 */
public interface BiTypedEventListener<T1, T2> extends IEventListener {
	public String getListenerName();

	@SuppressWarnings("unchecked")
	public default void received(Object... params) {
		try {
			if (params.length == 1) {
				received((T1) params[0]);
			} else if (params.length == 2) {
				received((T1) params[0], (T2) params[1]);
			}
		} catch (ClassCastException e) {			
		}
	}

	public default void received(T1 param) {}
	public void received(T1 param1, T2 param2);
}
