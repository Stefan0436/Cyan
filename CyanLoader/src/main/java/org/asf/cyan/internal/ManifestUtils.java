package org.asf.cyan.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import org.asf.cyan.internal.KickStartConfig.KickStartInstallation;

/**
 * 
 * A library to manipulate KickStart installation manifest files.<br/>
 * <br/>
 * This type is LGPL-3.0-licensed and may be copy-pasted into your own projects.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class ManifestUtils {

	private File configFile;
	private KickStartConfig config;

	private ManifestUtils(File file) throws IOException {
		configFile = file;

		config = new KickStartConfig();
		if (configFile.exists())
			config.readAll(Files.readString(configFile.toPath()));
		if (config.convert())
			write();
	}

	/**
	 * Retrieves a ManifestUtils instance connected to the default manifest file.
	 * 
	 * @return New ManifestUtils instance
	 * @throws IOException If reading the default manifest fails
	 */
	public static ManifestUtils getDefault() throws IOException {
		String dir = System.getenv("APPDATA");
		if (dir == null)
			dir = System.getProperty("user.home");

		File installs = new File(dir, ".kickstart-installer.ccfg");
		return new ManifestUtils(installs);
	}

	/**
	 * Retrieves a ManifestUtils instance connected to a specific file
	 * 
	 * @param file File to load
	 * @return New ManifestUtils instance
	 * @throws IOException If reading the default manifest fails
	 */
	public static ManifestUtils getForFile(File file) throws IOException {
		return new ManifestUtils(file);
	}

	/**
	 * Writes the current manifest to the configuration file
	 * 
	 * @throws IOException If saving fails
	 */
	public void write() throws IOException {
		Files.write(configFile.toPath(), config.toString().getBytes());
	}

	/**
	 * Retrieves the installations saved in the KickStart manifest.
	 * 
	 * @return Array of KickStartInstallation instances
	 */
	public KickStartInstallation[] getInstallations() {
		return config.registry;
	}

	/**
	 * Removes the given installation
	 * 
	 * @param installation Installation to remove
	 */
	public void removeInstallation(KickStartInstallation installation) {
		ArrayList<KickStartInstallation> installations = new ArrayList<KickStartInstallation>();
		for (KickStartInstallation install : config.registry) {
			if (install != installation)
				installations.add(install);
		}
		config.registry = installations.toArray(t -> new KickStartInstallation[t]);
	}

	/**
	 * Adds a new installation
	 * 
	 * @param profile               Installation profile name
	 * @param installationDirectory Installation directory path
	 * @param gameVersion           Installation game version
	 * @param platform              Installation platform ID
	 * @param platformVersion       Installation platform version
	 * @return New or existing KickStartInstallation instance
	 * @throws FileNotFoundException If the installation directory does not exist
	 */
	public KickStartInstallation addInstallation(String profile, File installationDirectory, String gameVersion,
			String platform, String platformVersion) throws FileNotFoundException {
		if (!hasInstallation(installationDirectory)) {
			if (!installationDirectory.exists())
				throw new FileNotFoundException(
						"Installation directory does not exist: " + installationDirectory.getPath());

			KickStartInstallation installation = new KickStartInstallation();
			installation.profileName = profile;
			installation.gameVersion = gameVersion;
			installation.platform = platform;
			installation.platformVersion = platformVersion;
			try {
				installation.installationDirectory = installationDirectory.getCanonicalPath();
			} catch (IOException e) {
				installation.installationDirectory = installationDirectory.getAbsolutePath();
			}
			installation.rootLoader = "generic";

			KickStartInstallation.Loader genericLoader = new KickStartInstallation.Loader();

			installation.loaders = new KickStartInstallation.Loader[] { genericLoader };

			addInstallation(installation);
			return installation;
		} else
			return getInstallation(installationDirectory);
	}

	/**
	 * Adds a new installation
	 * 
	 * @param installation Installation to add
	 */
	public void addInstallation(KickStartInstallation installation) {
		if (!hasInstallation(installation.installationDirectory)) {
			ArrayList<KickStartInstallation> installations = new ArrayList<KickStartInstallation>();
			for (KickStartInstallation inst : config.registry) {
				installations.add(inst);
			}
			installations.add(installation);
			config.registry = installations.toArray(t -> new KickStartInstallation[t]);
		}
	}

	/**
	 * Retrieves an installation by its installation directory path
	 * 
	 * @param installationDirectory Installation directory path
	 * @return KickStartInstallation instance or null
	 */
	public KickStartInstallation getInstallation(String installationDirectory) {
		return getInstallation(new File(installationDirectory));
	}

	/**
	 * Retrieves an installation by its installation directory
	 * 
	 * @param installationDirectory Installation directory
	 * @return KickStartInstallation instance or null
	 */
	public KickStartInstallation getInstallation(File installationDirectory) {
		String pth;
		try {
			pth = installationDirectory.getCanonicalPath();
		} catch (IOException e) {
			pth = installationDirectory.getAbsolutePath();
		}
		for (KickStartInstallation install : config.registry) {
			try {
				if (new File(install.installationDirectory).getCanonicalPath().equals(pth))
					return install;
			} catch (IOException e) {
				if (new File(install.installationDirectory).getAbsolutePath().equals(pth))
					return install;
			}
		}
		return null;
	}

	/**
	 * Checks if the given installation exists in the registry
	 * 
	 * @param installationDirectory Installation directory
	 * @return True if registered, false otherwise
	 */
	public boolean hasInstallation(File installationDirectory) {
		String pth;
		try {
			pth = installationDirectory.getCanonicalPath();
		} catch (IOException e) {
			pth = installationDirectory.getAbsolutePath();
		}
		for (KickStartInstallation install : config.registry) {
			try {
				if (new File(install.installationDirectory).getCanonicalPath().equals(pth))
					return true;
			} catch (IOException e) {
				if (new File(install.installationDirectory).getAbsolutePath().equals(pth))
					return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the given installation exists in the registry
	 * 
	 * @param installationDirectory Installation directory path
	 * @return True if registered, false otherwise
	 */
	public boolean hasInstallation(String installationDirectory) {
		return hasInstallation(new File(installationDirectory));
	}

}
