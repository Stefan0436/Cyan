package org.asf.cyan.api.internal.test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.config.ConfigManager;
import org.asf.cyan.api.internal.test.datafixers.FixerEvents;
import org.asf.cyan.api.internal.test.sides.ClientEvents;
import org.asf.cyan.api.internal.test.sides.ServerEvents;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
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

	protected static void initComponent() throws IOException {
		BaseEventController.addEventContainer(new TestEventListeners());
	}

	@AttachEvent(value = "mods.prestartgame", synchronize = true)
	private void attachAll(ClassLoader loader)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException {
		String tname1 = ServerEvents.class.getTypeName();
		String tname2 = ClientEvents.class.getTypeName();
		Class<?> cls1 = loader.loadClass(tname1);
		Class<?> cls2 = loader.loadClass(tname2);
		BaseEventController.addEventContainer((ServerEvents) cls1.getConstructor().newInstance());

		if (Modloader.getModloaderLaunchPlatform() != LaunchPlatform.MCP) { // Forge doesn't support datafixers
			BaseEventController.addEventContainer(new FixerEvents());
		}

		if (Modloader.getModloaderGameSide() == GameSide.CLIENT) {
			BaseEventController.addEventContainer((ClientEvents) cls2.getConstructor().newInstance());
		} else {
			ClientLanguage.registerLanguageKey("test.test", "hello world");
		}
	}

	@AttachEvent(value = "mods.preinit", synchronize = true)
	private void preInit() throws IOException {
		CyanLoader.testMod(this);
		@SuppressWarnings("unchecked")
		ConfigManager<TestEventListeners> manager = (ConfigManager<TestEventListeners>) ConfigManager
				.getFor(getClass());
		ModConfigTest config = manager.getConfiguration(ModConfigTest.class);
		config = config;
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
