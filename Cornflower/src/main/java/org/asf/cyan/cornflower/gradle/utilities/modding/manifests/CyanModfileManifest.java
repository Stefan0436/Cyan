package org.asf.cyan.cornflower.gradle.utilities.modding.manifests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Comment;
import org.asf.cyan.cornflower.gradle.tasks.CtcTask;
import org.asf.cyan.security.TrustContainer;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

public class CyanModfileManifest extends Configuration<CyanModfileManifest> implements IModManifest {

	private HashMap<Provider<RegularFile>, String> jarFiles = new HashMap<Provider<RegularFile>, String>();
	private HashMap<CtcTask, String> ctcTasks;

	public void setCtcTasks(HashMap<CtcTask, String> tasks) {
		ctcTasks = tasks;
	}

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	@Override
	public Map<Provider<RegularFile>, String> getJars() {
		return new HashMap<Provider<RegularFile>, String>(jarFiles);
	}

	@Comment("Main mod class name (simple name)")
	public String modClassName = null;

	@Comment("Main mod class package")
	public String modClassPackage = null;

	@Comment("Mod group")
	public String modGroup = null;

	@Comment("Mod id")
	public String modId = null;

	@Comment("Mod display name")
	public String displayName = null;

	@Comment("Mod version")
	public String version = null;

	@Comment("Game version regex")
	public String gameVersionRegex = null;

	@Comment("Message shown when version regex does not match, should be something like: 1.16.5 or greater")
	@Comment("The CYAN modloader prints the message like: ... wants <message>")
	public String gameVersionMessage = null;

	@Comment("Description language key (for the language files)")
	public String descriptionLanguageKey = null;

	@Comment("Fallback description if unavailable")
	public String fallbackDescription = null;

	@Comment("Mod jars, value is a check string, you can use: platform:<platform>, side:<side>, gameversion:<version> and loaderversion:<version>")
	@Comment("Note: the value string is not regex-based but absolute.")
	@Comment("")
	@Comment("You can use .. <check 1> & <check 2> ... for multiple 'checks'")
	@Comment("")
	@Comment("Use 'any' to allow on any platform.")
	public HashMap<String, String> jars = new HashMap<String, String>();

	@Comment("Dependency mods, the mods specified are required to run")
	@Comment("Format: mod-group:modid> 'version'")
	@Comment("")
	@Comment("The version is an advanced checkstring, it supplies the following options:")
	@Comment(" ~= regex   - rexex-based checking")
	@Comment(" < version  - less-than version")
	@Comment(" > version  - greater-than version")
	@Comment(" >= version - greater than or equal to version")
	@Comment(" <= version - less than or equal to version")
	@Comment(" version    - equal to version")
	public HashMap<String, String> dependencies = new HashMap<String, String>();

	@Comment("Optional dependencies, same format as normal dependencies")
	@Comment("but does not require the mods to be installed.")
	public HashMap<String, String> optionalDependencies = new HashMap<String, String>();

	@Comment("Mods and versions incompatible with this mod's version")
	public HashMap<String, String> incompatibilities = new HashMap<String, String>();

	@Comment("Map of supported mod loaders")
	@Comment("Disabled if empty, if used, only the specified mod loaders should attempt to load this mod.")
	@Comment("")
	@Comment("Uses the same format as dependencies.")
	@Comment("Mod loader IDs (lowercase only):")
	@Comment(" - Cyan: cyanloader")
	public HashMap<String, String> supportedModLoaders = new HashMap<String, String>();

	@Comment("Map of incompatible mod loaders")
	@Comment("The specified mod loaders should not be able to load this mod.")
	@Comment("If any are active in the user's installation, the loading process will be aborted.")
	@Comment("")
	@Comment("Uses the same format as dependencies.")
	@Comment("Mod loader IDs (case-insensitive):")
	@Comment(" - Cyan: cyanloader")
	@Comment(" - Forge: forge")
	@Comment(" - Fabric: fabric")
	@Comment(" - Paper: paper")
	public HashMap<String, String> incompatibleLoaderVersions = new HashMap<String, String>();

	@Comment("Maven repositories, format: name> url")
	public HashMap<String, String> mavenRepositories = new HashMap<String, String>();

	@Comment("Remote dependencies.")
	@Comment("Format:")
	@Comment("")
	@Comment("group> {")
	@Comment("    name> 'version'")
	@Comment("}")
	public HashMap<String, HashMap<String, String>> mavenDependencies = new HashMap<String, HashMap<String, String>>();

	@Comment("Trust container files")
	@Comment("Format: name@version> 'remote url'")
	public HashMap<String, String> trustContainers = new HashMap<String, String>();

	@Comment("Platform configuration")
	@Comment("Format: platform> 'checkstring'")
	@Comment("")
	@Comment("Same checkstring system as dependencies")
	public HashMap<String, String> platforms = new HashMap<String, String>();

	@Comment("The update server for this mod, updates are downloaded from this URL")
	public String updateserver = null;

	@Override
	public void addJar(Provider<RegularFile> jar, String platform, String side, String outDir, String loaderVersion,
			String gameVersion, String mappingsVersion) {
		String checkString = "any";

		if (platform != null) {
			if (checkString.equals("any"))
				checkString = "";

			checkString += " platform:" + platform;
		}
		if (side != null) {
			if (checkString.equals("any"))
				checkString = "";
			else
				checkString += " &";

			checkString += " side:" + side;
		}
		if (loaderVersion != null) {
			if (checkString.equals("any"))
				checkString = "";
			else
				checkString += " &";

			checkString += " loaderversion:" + loaderVersion;
		}
		if (gameVersion != null) {
			if (checkString.equals("any"))
				checkString = "";
			else
				checkString += " &";

			checkString += " gameversion:" + gameVersion;
		}
		if (mappingsVersion != null && (platform == null || !platform.equals("DEOBFUSCATED"))) {
			if (checkString.equals("any"))
				checkString = "";
			else
				checkString += " &";

			checkString += " mappingsversion:" + mappingsVersion;
		}
		if (!checkString.equals("any"))
			checkString += " ";

		this.jars.put(outDir + "/" + jar.get().getAsFile().getName(), checkString);
		this.jarFiles.put(jar, outDir + "/" + jar.get().getAsFile().getName());
	}

	@Override
	public String toString() {
		return toString(true);
	}

	public String toString(boolean b) {
		if (b) {
			if (ctcTasks != null)
				for (CtcTask ctc : ctcTasks.keySet()) {
					try {
						String dest = ctcTasks.get(ctc);
						if (ctc.getVersion() == null)
							throw new IOException("CTC task " + ctc.getName()
									+ " has not been run yet or does not use the 'pack' method.");

						TrustContainer cont = TrustContainer.importContainer(ctc.getOutput());
						trustContainers.put(cont.getContainerName() + "@" + cont.getVersion(), dest);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
		}

		return super.toString();
	}

}
