package org.asf.cyan.internal;

import java.io.File;
import java.util.ArrayList;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Comment;
import org.asf.cyan.api.modloader.information.game.GameSide;

/**
 * KickStart Installation Manifest.<br/>
 * <br/>
 * This type is LGPL-3.0-licensed and may be copy-pasted into your own projects.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@Comment("")
@Comment("KickStart Installation Manifest")
@Comment("To append modloaders, pelase use the ManifestUtils type embedded in the KickStart Installer.")
@Comment("")
@Comment("Format: CCFG")
@Comment(" ")
public class KickStartConfig extends Configuration<KickStartConfig> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public String kickStartVersion = "0.0";
	public String formatVersion = "5.11";

	public KickStartInstallation[] registry = new KickStartInstallation[0];
	public KickStartOldInstallation[] installations = null;

	public boolean convert() {
		boolean changes = false;
		if (installations != null) {
			formatVersion = "0.0";
			kickStartVersion = "0.0";
		}

		if (!kickStartVersion.equals("5.11")) {
			while (true) {
				boolean done = false;
				switch (formatVersion) {
				case "0.0":
					for (KickStartOldInstallation installation : installations) {
						if (!new File(installation.cyanData).exists())
							continue;

						KickStartInstallation install = new KickStartInstallation();
						install.gameVersion = installation.gameVersion;
						install.side = installation.side;
						install.installationDirectory = new File(installation.cyanData).getParent();
						install.rootLoader = "cyanloader";
						install.platform = installation.platform;
						install.platformVersion = installation.platformVersion;

						KickStartInstallation.Loader cyanLoader = new KickStartInstallation.Loader();
						cyanLoader.ID = "cyanloader";
						cyanLoader.modInstallDir = ".cyan-data/mods";
						cyanLoader.coreModInstallDir = ".cyan-data/coremods";
						cyanLoader.name = "Cyan";
						cyanLoader.version = installation.loaderVersion;
						install.loaders = new KickStartInstallation.Loader[] { cyanLoader };

						registry = ArrayUtil.append(registry, new KickStartInstallation[] { install });
					}
					installations = null;
					formatVersion = "5.11";
					changes = true;
					break;
				default:
					done = true;
					break;
				}
				if (done)
					break;
			}

			kickStartVersion = "5.11";
		}

		ArrayList<KickStartInstallation> installations = new ArrayList<KickStartInstallation>();
		for (KickStartInstallation install : registry) {
			if (new File(install.installationDirectory).exists())
				installations.add(install);
		}
		registry = installations.toArray(t -> new KickStartInstallation[t]);

		return changes;
	}

	/**
	 * 
	 * Intallation metadata type
	 * 
	 * @author Sky Swimmer - AerialWorks Software Foundation
	 *
	 */
	public static class KickStartInstallation extends Configuration<KickStartInstallation> {

		@Override
		public String filename() {
			return null;
		}

		@Override
		public String folder() {
			return null;
		}

		public String profileName;
		public String gameVersion;

		public GameSide side;

		public String installationDirectory;

		public String rootLoader;
		public Loader[] loaders = new Loader[0];

		public String platform;
		public String platformVersion;

		/**
		 * Retrieves the root modloader of the current installation
		 * 
		 * @return Loader instance (returns a generic loader if not found)
		 */
		public Loader getRootLoader() {
			if (rootLoader != null) {
				for (Loader ld : loaders) {
					if (ld.ID.equals(rootLoader)) {
						return ld;
					}
				}
			}

			Loader loader = new Loader();
			loader.ID = "generic";
			loader.name = "Generic Modlaoder";
			loader.modInstallDir = "mods";
			loader.coreModInstallDir = "mods";
			return loader;
		}

		/**
		 * Checks if the given loader ID is present in the current installation
		 * 
		 * @param id Modloader ID to check
		 * @return True if present, false otherwise
		 */
		public boolean hasLoader(String id) {
			for (Loader ld : loaders) {
				if (ld.ID.equals(id)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Retieves a mod loader by ID
		 * 
		 * @param id Modloader ID
		 * @return Loader instance or null
		 */
		public Loader getLoader(String id) {
			for (Loader ld : loaders) {
				if (ld.ID.equals(id)) {
					return ld;
				}
			}
			return null;
		}

		/**
		 * Appends a new modloader
		 * 
		 * @param ld Loader to append
		 * @return Loader instance (retrieves existing modloader if one is present)
		 */
		public Loader appendLoader(Loader ld) {
			if (hasLoader(ld.ID)) {
				getLoader(ld.ID).version = ld.version;
				return getLoader(ld.ID);
			}

			ArrayList<Loader> loaders = new ArrayList<Loader>();
			for (Loader ldr : this.loaders)
				loaders.add(ldr);
			loaders.add(ld);
			this.loaders = loaders.toArray(t -> new Loader[t]);

			return ld;
		}

		/**
		 * Appends a new modloader
		 * 
		 * @param ID          Modloader ID
		 * @param name        Modloader name
		 * @param version     Modloader version
		 * @param modsDir     Mods directory sub-path
		 * @param coremodsDir Coremods directory sub-path
		 * @return Loader instance
		 */
		public Loader appendLoader(String ID, String name, String version, String modsDir, String coremodsDir) {
			Loader loader = new Loader();
			loader.ID = ID;
			loader.name = name;
			loader.version = version;
			loader.modInstallDir = modsDir;
			loader.coreModInstallDir = coremodsDir;
			return appendLoader(loader);
		}

		/**
		 * Appends a new modloader
		 * 
		 * @param ID          Modloader ID
		 * @param name        Modloader name
		 * @param version     Modloader version
		 * @param modsDir     Mods directory
		 * @param coremodsDir Coremods directory
		 * @return Loader instance
		 */
		public Loader appendLoader(String ID, String name, String version, File modsDir, File coremodsDir) {
			Loader loader = new Loader();
			loader.ID = ID;
			loader.name = name;
			loader.version = version;
			loader.modInstallDir = modsDir.getPath();
			loader.coreModInstallDir = coremodsDir.getPath();
			return appendLoader(loader);
		}

		/**
		 * Appends a new modloader
		 * 
		 * @param ID      Modloader ID
		 * @param name    Modloader name
		 * @param version Modloader version
		 * @param modsDir Mods directory
		 * @return Loader instance
		 */
		public Loader appendLoader(String ID, String name, String version, File modsDir) {
			Loader loader = new Loader();
			loader.ID = ID;
			loader.name = name;
			loader.version = version;
			loader.modInstallDir = modsDir.getPath();
			loader.coreModInstallDir = modsDir.getPath();
			return appendLoader(loader);
		}

		/**
		 * Appends a new modloader
		 * 
		 * @param ID      Modloader ID
		 * @param name    Modloader name
		 * @param version Modloader version
		 * @param modsDir Mods directory sub-path
		 * @return Loader instance
		 */
		public Loader appendLoader(String ID, String name, String version, String modsDir) {
			Loader loader = new Loader();
			loader.ID = ID;
			loader.name = name;
			loader.version = version;
			loader.modInstallDir = modsDir;
			loader.coreModInstallDir = modsDir;
			return appendLoader(loader);
		}

		/**
		 * Appends a new modloader
		 * 
		 * @param ID      Modloader ID
		 * @param name    Modloader name
		 * @param version Modloader version
		 * @return Loader instance
		 */
		public Loader appendLoader(String ID, String name, String version) {
			Loader loader = new Loader();
			loader.ID = ID;
			loader.name = name;
			loader.version = version;
			loader.modInstallDir = "mods";
			loader.coreModInstallDir = "mods";
			return appendLoader(loader);
		}

		/**
		 * Clears the list of mod loaders
		 */
		public void clearLoaders() {
			loaders = new Loader[0];
		}

		/**
		 * 
		 * Modloader metadata type
		 * 
		 * @author Sky Swimmer - AerialWorks Software Foundation
		 *
		 */
		public static class Loader extends Configuration<Loader> {

			@Override
			public String filename() {
				return null;
			}

			@Override
			public String folder() {
				return null;
			}

			public String ID = "";
			public String name = "";
			public String version = "";

			public String coreModInstallDir = "";
			public String modInstallDir = "";

		}

	}

	/**
	 * 
	 * Legacy installation type, used for format convertion
	 * 
	 * @author Sky Swimmer - AerialWorks Software Foundation
	 *
	 */
	public static class KickStartOldInstallation extends Configuration<KickStartOldInstallation> {

		@Override
		public String filename() {
			return null;
		}

		@Override
		public String folder() {
			return null;
		}

		public GameSide side;

		public String cyanData;
		public String gameVersion;

		public String loaderVersion;
		public String platform;
		public String platformVersion;

	}

}
