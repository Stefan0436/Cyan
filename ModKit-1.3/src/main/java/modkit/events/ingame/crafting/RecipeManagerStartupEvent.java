package modkit.events.ingame.crafting;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.ingame.crafting.RecipeManagerEventObject;

/**
 * 
 * Recipe Manager Startup Event -- Called on recipe manager startup.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class RecipeManagerStartupEvent extends AbstractExtendedEvent<RecipeManagerEventObject> {

	private static RecipeManagerStartupEvent implementation;

	@Override
	public String channelName() {
		return "modkit.start.crafting.recipe.manager";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static RecipeManagerStartupEvent getInstance() {
		return implementation;
	}

}
