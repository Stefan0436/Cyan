package org.asf.cyan.mods;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;
import org.asf.cyan.mods.config.CyanModfileManifest;

/**
 * 
 * Mod interface, its recommended to use AbstractMod instead.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IMod extends IBaseMod {
	public default URL getResource(String path) {
		String base = getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		if (base.toString().startsWith("jar:"))
			base = base.substring(0, base.lastIndexOf("!"));
		else if (base.endsWith("/" + getClass().getTypeName().replace(".", "/") + ".class")) {
			base = base.substring(0,
					base.length() - ("/" + getClass().getTypeName().replace(".", "/") + ".class").length());
		}

		try {
			if ((base.endsWith(".jar") || base.endsWith(".zip")) && !base.startsWith("jar:"))
				base = "jar:" + base + "!";
			new URL(base + "/" + path).openStream().close();
			return new URL(base + "/" + path);
		} catch (IOException e) {
			return null;
		}
	}

	public default InputStream getResourceAsStream(String path) {
		String base = getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		if (base.toString().startsWith("jar:"))
			base = base.substring(0, base.lastIndexOf("!"));
		else if (base.endsWith("/" + getClass().getTypeName().replace(".", "/") + ".class")) {
			base = base.substring(0,
					base.length() - ("/" + getClass().getTypeName().replace(".", "/") + ".class").length());
		}

		try {
			if ((base.endsWith(".jar") || base.endsWith(".zip")) && !base.startsWith("jar:"))
				base = "jar:" + base + "!";
			return new URL(base + "/" + path).openStream();
		} catch (IOException e) {
			return null;
		}
	}

	public void setup(Modloader modloader, GameSide side, CyanModfileManifest manifest);

	public void setLanguageBasedDescription(String description);

	public String getDescriptionLanguageKey();

	public void setDefaultDescription();
}
