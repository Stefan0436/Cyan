package org.asf.cyan;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.modloader.information.providers.IModProvider;
import org.asf.cyan.api.modloader.information.providers.IVersionProvider;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.internal.BaseEventController;

import modkit.protocol.handshake.HandshakeRule;
import modkit.threading.ModThread;
import modkit.threading.ThreadManager;

public class TestComponent extends CyanComponent implements IPostponedComponent, IBaseMod, IEventListenerContainer {

	private class TestLoader extends Modloader implements IModProvider, IVersionProvider {

		public TestComponent comp;

		public TestLoader(TestComponent comp) {
			this.comp = comp;
			Modloader.appendImplementation(this);
			this.addInformationProvider(this);
		}

		@Override
		protected String getImplementationName() {
			return "TestLoader";
		}

		@Override
		public String getSimpleName() {
			return "Test";
		}

		@Override
		public String getName() {
			return "TestLoader";
		}

		@Override
		public int getAllKnownModsLength() {
			return 1;
		}

		@Override
		public IModManifest[] getLoadedNormalMods() {
			return new IModManifest[] { comp.getManifest() };
		}

		@Override
		public IModManifest[] getLoadedCoreMods() {
			return new IModManifest[0];
		}

		@Override
		public Version getLoaderVersion() {
			return Version.fromString("1.0.0.0");
		}

	}

	@Override
	public void initComponent() {
		new TestLoader(this);
		BaseEventController.addEventContainer(this);
		HandshakeRule.registerRule(new HandshakeRule(GameSide.CLIENT, "testmod", "1.0.0.A4"));
		HandshakeRule.registerRule(new HandshakeRule(GameSide.SERVER, "testmod", "1.0.0.A4"));
	}

	@AttachEvent(value = "mods.preinit", synchronize = true)
	public void preInit() throws InterruptedException {
		ThreadManager manager = ThreadManager.createManager(this);
		ModThread th = manager.createThread("Test thread");
		th.setRepeating(true);
		th.sleep(400, false);
		manager.createThread().store();
		manager.createThread().suspend();
	}

	@Override
	public IModManifest getManifest() {
		return new IModManifest() {

			@Override
			public String id() {
				return "testmod";
			}

			@Override
			public String displayName() {
				return "Test Mod";
			}

			@Override
			public Version version() {
				return Version.fromString("1.0.0.A4");
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
				return "A Testing Mod";
			}
		};
	}

}
