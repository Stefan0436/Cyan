package org.asf.cyan.api.internal.test;

import java.io.IOException;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.config.ConfigManager;
import org.asf.cyan.api.internal.test.sides.ClientEvents;
import org.asf.cyan.api.internal.test.sides.ServerEvents;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.IMod;
import org.asf.cyan.mods.config.CyanModfileManifest;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.internal.BaseEventController;

@CYAN_COMPONENT
public class TestEventListeners extends CyanComponent implements IEventListenerContainer, IMod {

	public TestEventListeners() throws IOException {
		CyanLoader.testMod(this);
		@SuppressWarnings("unchecked")
		ConfigManager<TestEventListeners> manager = (ConfigManager<TestEventListeners>) ConfigManager
				.getFor(getClass());
		ModConfigTest config = manager.getConfiguration(ModConfigTest.class);
		config = config;
	}

	protected static void initComponent() throws IOException {
		BaseEventController.addEventContainer(new TestEventListeners());
		BaseEventController.addEventContainer(new ServerEvents());
		if (Modloader.getModloaderGameSide() == GameSide.CLIENT) {
			BaseEventController.addEventContainer(new ClientEvents());
		} else {
			ClientLanguage.registerLanguageKey("test.test", "hello world");
		}
	}

	@AttachEvent("mods.preinit")
	private void preInit() {
		this.equals(this); // OK
	}

	@AttachEvent("mods.init")
	private void init() {
		this.equals(this); // OK
	}

	@AttachEvent("mods.postinit")
	private void postInit() {
		this.equals(this); // OK
	}

	@AttachEvent("mods.runtimestart")
	private void runtime() {
		this.equals(this); // OK
	}

	@Override
	public IModManifest getManifest() {
		return new IModManifest() {

			@Override
			public String id() {
				return "testmod:test";
			}

			@Override
			public String displayName() {
				return "Test Mod";
			}

			@Override
			public Version version() {
				return Version.fromString("1.0.0.0");
			}

			@Override
			public String[] dependencies() {
				return new String[0];
			}

			@Override
			public String[] optionalDependencies() {
				return new String[0];
			}

			@Override
			public String description() {
				return "Test mod";
			}

		};
	}

	@Override
	public void setup(Modloader modloader, GameSide side, CyanModfileManifest manifest) {

	}

	@Override
	public void setLanguageBasedDescription(String description) {

	}

	@Override
	public String getDescriptionLanguageKey() {
		return null;
	}

	@Override
	public void setDefaultDescription() {

	}

}
