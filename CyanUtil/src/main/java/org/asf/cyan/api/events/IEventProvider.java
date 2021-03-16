package org.asf.cyan.api.events;

/**
 * 
 * Event channel providers, a way so that core mods can add their own
 * events.<br />
 * A modloader needs to implement this system in order for it to work,
 * CyanLoader uses component presentation to do this.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IEventProvider {
	public String getChannelName();
}
