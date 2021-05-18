package org.example.examplemod;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.api.util.ContainerConditions;
import org.asf.cyan.api.util.EventUtil;
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

	//
	// Note:
	// Since Cyan 1.0.0.A13, the game.beforestart event won't be called after mods
	// are loaded, it gets called before then and cannot be used anymore.
	//
	// You can instead, make use of the mods.preinit method.
	//
	//
	//
	// What are events?
	// Cyan, just like Forge and Spigot are, is an event-based modloader. Mods can
	// interact with the game by 'attaching' to ModKit events.
	//
	//
	// You can attach to a event by using the following annotations:
	// Asynchronous events (default): @AttachEvent("event.id")
	// Synchronous events: @AttachEvent(value = "event.id", synchronize = true)
	//
	// Specifying synchronize = true will tell the game to wait for the code to
	// complete.
	//
	// This means, if you use @AttachEvent("mods.preinit") without any parameters,
	// the game will continue loading without waiting for the mod to finish its
	// event code. This can be useful, but not with the init methods.
	//
	//
	// IMPORTANT NOTICE:
	// Unlike normal mods, coremods are loaded by Cyan's own class loader and not
	// the game's class loader. This can cause issues with forge and fabric, please
	// do not use game code in the main coremod class.
	//
	// Use the EventUtil system to register events containers loaded by the game's
	// class loader. (see next event listener)
	//
	@AttachEvent(value = "mods.preinit", synchronize = true)
	public void preInit() { // Method parameters define what types the event passes on to the handler
		// Use this event for interacting with the game in its earliest stage
	}

	//
	// We add this event to make sure our events are loaded before EventUtil
	// registers the events
	//
	// The alternate mods.all.loaded event provides the game class loader, but we
	// cannot use it for events as EventUtil is called by it.
	@AttachEvent(value = "mods.all.loaded", synchronize = true)
	public void afterAllMods() {

		//
		// We keep our class package in memory to use it in the event registry.
		String pkg = getClass().getPackageName();

		//
		// The following registers the ClientEvents container,
		// ContainerConditions.CLIENT tells the ModKit to only load this container on
		// the client. Containers added through the EventUtil library are loaded by the
		// game and not by cyan, this allows coremods to use game code on fabric and
		// forge. With Paper and Cyan itself, you don't have to worry about it.
		//
		// But Cyan mods are intended to be most compatible, so it is recomemnded to use
		// the EventUtil system.
		//
		// If you are using regular mods, you won't have to worry, regular mods are
		// loaded by the game and not cyan.
		//
		// Registers the client event container:
		EventUtil.registerContainer(ContainerConditions.CLIENT, () -> pkg + ".events.ClientEvents");

		//
		// Added on both sides, the client uses an integrated server, so use COMMON to
		// register items, blocks, entities and such. Use the CLIENT events to register
		// renderers and keyboard handlers. (key bindings are not yet implemented)
		EventUtil.registerContainer(ContainerConditions.COMMON, () -> pkg + ".events.CommonEvents");

		//
		// Note:
		// Coremods should not contain custom items, blocks, entities, dimensions and
		// other game extensions. Please use regular mods for that.
	}
}
