package org.asf.cyan;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Comment;

public class CyanModfileManifest extends Configuration<CyanModfileManifest>{
	
	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
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

}
