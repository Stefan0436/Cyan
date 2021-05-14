package org.asf.cyan;

import org.asf.cyan.api.versioning.Version;

public class CheckString {

	public static boolean validateCheckString(String checkString, Version version) {
		return validateCheckString(checkString, version, "", true) == null;
	}

	public static String validateCheckString(String check, Version version, String message, boolean brief) {
		String error = "";
		for (String checkVersion : check.split(" | ")) {
			error = null;
			for (String checkStr : checkVersion.split(" & ")) {
				checkStr = checkStr.trim();
				if (checkStr.startsWith("~=")) {
					String regex = checkStr.substring(2);
					if (regex.startsWith(" "))
						regex = regex.substring(1);
					if (!version.toString().matches(regex)) {
						if (!brief)
							error = message + " (incompatible version installed)";
						else
							error = message + " (incompatible)";
						break;
					}
				} else if (checkStr.startsWith(">=")) {
					String str = checkStr.substring(2);
					if (str.startsWith(" "))
						str = str.substring(1);

					Version min = Version.fromString(str);

					if (!version.isGreaterOrEqualTo(min)) {
						if (!brief)
							error = message + " (outdated version installed)";
						else
							error = message + " (>= " + str + ")";
						break;
					}
				} else if (checkStr.startsWith("<=")) {
					String str = checkStr.substring(2);
					if (str.startsWith(" "))
						str = str.substring(1);

					Version min = Version.fromString(str);

					if (!version.isLessOrEqualTo(min)) {
						if (!brief)
							error = message + " (incompatible newer version installed)";
						else
							error = message + " (<= " + str + ")";
						break;
					}
				} else if (checkStr.startsWith(">")) {
					String str = checkStr.substring(1);
					if (str.startsWith(" "))
						str = str.substring(1);

					Version min = Version.fromString(str);

					if (!version.isGreaterThan(min)) {
						if (!brief)
							error = message + " (outdated version installed)";
						else
							error = message + " (" + str + "+)";
						break;
					}
				} else if (checkStr.startsWith("<")) {
					String str = checkStr.substring(1);
					if (str.startsWith(" "))
						str = str.substring(1);

					Version min = Version.fromString(str);

					if (!version.isLessThan(min)) {
						if (!brief)
							error = message + " (incompatible newer version installed)";
						else
							error = message + " (Pre-" + str + ")";
						break;
					}
				} else if (checkStr.startsWith("!=")) {
					if (version.isEqualTo(Version.fromString(checkStr.substring(2).trim()))) {
						if (!brief)
							error = message + " (incompatible version installed)";
						else
							error = message + " (" + checkStr.substring(2).trim() + ")";
						break;
					}
				} else {
					if (!version.isEqualTo(Version.fromString(checkStr.trim()))) {
						if (!brief)
							error = message + " (incompatible version installed)";
						else
							error = message + " (" + checkStr.trim() + ")";
						break;
					}
				}
			}
			if (error == null)
				break;
		}
		return error;
	}

}
