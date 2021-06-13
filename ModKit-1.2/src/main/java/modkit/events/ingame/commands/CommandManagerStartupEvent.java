package modkit.events.ingame.commands;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.ingame.commands.CommandManagerEventObject;

/**
 * 
 * Command Manager Startup Event -- Called on command manager startup.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CommandManagerStartupEvent extends AbstractExtendedEvent<CommandManagerEventObject> {

	private static CommandManagerStartupEvent implementation;

	@Override
	public String channelName() {
		return "modkit.start.commands.manager";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static CommandManagerStartupEvent getInstance() {
		return implementation;
	}

}
