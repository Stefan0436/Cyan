package org.asf.cyan.api.events.core;

/**
 * 
 * Modloader Event Listener
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IEventListener {
	public String getListenerName();
	public void received(Object... params);
}
