package org.example;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.util.ContainerConditions;
import org.asf.cyan.api.util.EventUtil;
import org.asf.cyan.mods.AbstractMod;
import org.asf.cyan.mods.config.CyanModfileManifest;
import org.asf.cyan.mods.events.AttachEvent;

public class ExampleMod extends AbstractMod {

	@Override
	public void setup(Modloader modloader, GameSide side, CyanModfileManifest manifest) {
		super.setup(modloader, side, manifest);

		// Setup is called to configure the mod, the super setup method is needed
		// to load the mod properties of the AbstractMod type.

		// We use this to register our event containers
		// Use common instead of the server condition, because the client uses an integrated server.
		
		// Common:
		EventUtil.registerContainer(ContainerConditions.COMMON, this::commonEvents);
		EventUtil.registerContainer(ContainerConditions.COMMON, this::dataFixerEvents);
		
		// Client-only:
		EventUtil.registerContainer(ContainerConditions.CLIENT, this::clientEvents);
		
		// Server-only (not implemented in mod, not recommended, but does work if you want these events):
		// EventUtil.registerContainer(ContainerConditions.SERVER, this::serverEvents);
	}

	// We want the event preInit to by called synchronously,
	// this makes sure the game waits for our mod to be fully loaded.
	@AttachEvent(value = "mods.preinit", synchronize = true)
	public void preInit() {
		// Called on game startup, we want to use this to register our mod handshake rules.
		// We also register our client language keys in this event.
		
		// If your mod adds blocks, items or entities, it needs to be loaded on BOTH client and server.
		addHandshakeRule(this);
		
		//
		// Client-side only rule: when joining a sever this mod needs to be present
		// on the client. It does not need to be present on the server if installed on the client.
		// addHandshakeRule(GameSide.CLIENT, this);
		
		// Server-side only rule: client requires the mod to be installed on the server,
		// If the mod is not installed, the client will disconnect immediately.
		// addHandshakeRule(GameSide.CLIENT, this);
		
		// Specifying no version will require the same version on both sides.
		// You can replace 'this' with another mod instance or id with version/string values.
		// Examples:
		// addHandshakeRule("testmod:test", ">=greater-or-equal-to-version");
		// addHandshakeRule("testmod:test", "<=less-or-equal-to-version");
		// addHandshakeRule("testmod:test", ">greater-than-version");
		// addHandshakeRule("testmod:test", "<less-than-version");
		// addHandshakeRule("testmod:test", "~=regex-version");
		// addHandshakeRule("testmod:test", "version");
	}

	private String commonEvents() {
		return getClass().getPackageName() + ".events.CommonEvents";
	}

	private String clientEvents() {
		return getClass().getPackageName() + ".events.ClientEvents";
	}

	private String dataFixerEvents() {
		return getClass().getPackageName() + ".events.Fixers";
	}

}
