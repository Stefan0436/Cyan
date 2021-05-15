package org.asf.cyan;

import java.io.IOException;
import javax.swing.JOptionPane;

public class InstallerMain {

	public static void main(String[] args) throws IOException {
		String jv = System.getProperty("java.version");
		if (jv.contains("."))
			jv = jv.substring(0, jv.indexOf("."));
		int java = Integer.valueOf(jv);
		if (java < 11) {
			JOptionPane.showMessageDialog(null, "The KickStart installer requires JAVA 11+\nPlease use a newer JVM.", "Cannot install",
					JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
		run(args);
	}
	
	public static void run(String[] args) throws IOException {
		Installer.main(args);
	}

}
