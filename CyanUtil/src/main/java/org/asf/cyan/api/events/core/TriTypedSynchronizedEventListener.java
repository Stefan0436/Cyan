package org.asf.cyan.api.events.core;

/**
 * 
 * Modloader Event Listener with three type arguments that does not run in its own thread.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 * 
 */
public interface TriTypedSynchronizedEventListener<T1, T2, T3> extends ISynchronizedEventListener {
	public String getListenerName();

	@SuppressWarnings("unchecked")
	public default void received(Object... params) {
		try {
			if (params.length == 1) {
				received((T1) params[0]);
			} else if (params.length == 2) {
				received((T1) params[0], (T2) params[1]);
			} else if (params.length == 3) {
				received((T1) params[0], (T2) params[1], (T3) params[2]);
			}
		} catch (ClassCastException e) {			
		}
	}

	public default void received(T1 param) {}
	public default void received(T1 param1, T2 param2) {}
	public void received(T1 param1, T2 param2, T3 param3);
}
