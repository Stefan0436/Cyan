package modkit.events.objects.ingame.crafting;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.world.item.crafting.RecipeManager;

/**
 * 
 * Recipe Manager Event Object -- Event object for all events related to the
 * recipe manager.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class RecipeManagerEventObject extends EventObject {
	private RecipeManager recipeManager;

	public RecipeManagerEventObject(RecipeManager recipeManager) {
		this.recipeManager = recipeManager;
	}

	/**
	 * Retrieves the recipe manager
	 */
	public RecipeManager getRecipeManager() {
		return recipeManager;
	}

}
