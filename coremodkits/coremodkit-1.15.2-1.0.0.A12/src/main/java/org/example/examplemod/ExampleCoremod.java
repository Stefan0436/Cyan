package org.example.examplemod;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.mods.AbstractCoremod;
import org.asf.cyan.mods.events.AttachEvent;

// We target any modloader with our coremod, Cyan should not be referenced directly
@TargetModloader(value = Modloader.class, any = true)
public class ExampleCoremod extends AbstractCoremod {

	@Override
	// Main coremod setup method
	protected void setupCoremod() {
		// Sets up the coremod,
		// In our case, prints Hello World to the log.
		info("Hello World!");
	}

	// Event listener for the game.beforestart event
	// Parameters must match the event parameters. (naming does not matter)
	// In this case, we receive the main type and the game arguments.
	@AttachEvent("game.beforestart")
	public void beforeStart(String mainClass, String[] arguments) {
		info("Coremod: main class: " + mainClass);
	}

}
