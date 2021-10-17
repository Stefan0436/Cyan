package org.asf.cyan;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.installations.KickStartConfig;
import org.asf.cyan.installations.KickStartConfig.KickStartInstallation;
import org.asf.cyan.installations.KickStartConfig.KickStartInstallation.Loader;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SelectionWindow extends JFrame {
	private static final long serialVersionUID = 5938796943958873974L;
	private JPanel contentPane;
	private boolean closed = false;
	private File resultDir;
	private File file;
	private JComboBox<InstallItem> comboBox;

	private ProjectConfig config = new ProjectConfig();
	private CyanModfileManifest mf;

	private class InstallItem {
		public String dsp;
		public KickStartInstallation installation;
		public File instdir;

		@Override
		public String toString() {
			return dsp;
		}
	}

	private void load() throws IOException {
		String dir = System.getenv("APPDATA");
		if (dir == null)
			dir = System.getProperty("user.home");

		File installs = new File(dir, ".kickstart-installer.ccfg");
		if (!installs.exists()) {
			JOptionPane.showMessageDialog(this,
					"No " + config.name.toUpperCase() + " installations present, please install "
							+ config.name.toUpperCase() + " first.",
					"Missing installations", JOptionPane.WARNING_MESSAGE);
			closed = true;
			dispose();
			return;
		}

		mf = getModManifest(file);
		KickStartConfig conf = new KickStartConfig();
		conf.readAll(new String(Files.readAllBytes(installs.toPath())));
		if (conf.convert())
			Files.write(installs.toPath(), conf.toString().getBytes());
		for (KickStartInstallation install : conf.registry) {
			if (checkInstall(mf, install)) {
				File data = new File(install.installationDirectory);
				InstallItem inst = new InstallItem();
				String name = install.profileName;
				inst.installation = install;
				if (name == null)
					name = data.getName();
				inst.dsp = "[" + install.platform + "] [" + install.side + "] " + name + " (" + install.gameVersion
						+ (install.getRootLoader().version != null ? ", " + install.getRootLoader().version : "") + ")";
				inst.instdir = data;
				if (inst.instdir.exists())
					comboBox.addItem(inst);
			}
		}

		if (comboBox.getItemCount() == 0) {
			JOptionPane.showMessageDialog(this,
					"No compatible " + config.name.toUpperCase() + " installations present, please install "
							+ config.name.toUpperCase() + " first.",
					"Missing installations", JOptionPane.WARNING_MESSAGE);
			closed = true;
			dispose();
			return;
		} else if (comboBox.getItemCount() == 1) {
			resultDir = comboBox.getItemAt(0).instdir;
			closed = true;
			dispose();
			return;
		}
	}

	public static File showWindow(File file) throws IOException {
		SelectionWindow frame = new SelectionWindow();
		frame.file = file;
		frame.load();
		if (!file.exists())
			return null;
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent arg0) {
				frame.closed = true;
			}

		});
		if (!frame.closed)
			frame.setVisible(true);

		while (!frame.closed) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}

		return frame.resultDir;
	}

	public boolean checkInstall(CyanModfileManifest manifest, KickStartInstallation install) {
		if (manifest.gameVersionRegex != null && !install.gameVersion.matches(manifest.gameVersionRegex))
			return false;

		boolean coremod = false;
		if (file.getName().endsWith(".ccmf")) {
			coremod = true;
		}

		if (manifest.supportedModLoaders.size() != 0) {
			boolean compatible = false;
			for (String loader : manifest.supportedModLoaders.keySet()) {
				if (install.hasLoader(loader)) {
					Loader ld = install.getLoader(loader);
					Version v = Version.fromString(ld.version);
					if (CheckString.validateCheckString(manifest.supportedModLoaders.get(loader), v)) {
						compatible = true;
						break;
					}
				}
			}
			if (compatible) {
				for (String loader : manifest.incompatibleLoaderVersions.keySet()) {
					if (install.hasLoader(loader)) {
						Version v = Version.fromString(install.getLoader(loader).version);
						if (CheckString.validateCheckString(manifest.incompatibleLoaderVersions.get(loader), v)) {
							compatible = false;
							break;
						}
					}
				}
			}
			if (!compatible)
				return false;
		} else if (coremod && install.hasLoader("cyanloader")
				&& Version.fromString(install.getLoader("cyanloader").version)
						.isGreaterOrEqualTo(Version.fromString("1.0.0.A15"))) {
			return false;
		}

		if (install.platform.equals("DEOBFUSCATED")
				&& manifest.jars.values().stream().anyMatch(t -> t.trim().equals("any")
						|| (" " + t.trim() + " ").replace("&", " & ").contains(" platform:DEOBFUSCATED "))) {
			return true;
		}
		if (!manifest.platforms.containsKey(install.platform))
			return false;
		else
			return CheckString.validateCheckString(manifest.platforms.get(install.platform),
					Version.fromString(install.platformVersion));
	}

	public static CyanModfileManifest getModManifest(File file) throws IOException {
		String ccfg = null;
		try {
			InputStream strm = new URL("jar:" + file.toURI().toURL() + "!/mod.manifest.ccfg").openStream();
			ccfg = new String(strm.readAllBytes());
			strm.close();
		} catch (IOException e) {
		}
		if (ccfg == null) {
			throw new IOException("Invalid mod file, missing manifest.");
		}
		return new CyanModfileManifest().readAll(ccfg);
	}

	/**
	 * Create the frame.
	 */
	public SelectionWindow() throws IOException {
		setTitle("Select a " + config.name.toUpperCase() + " installation...");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 627, 173);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setLocationRelativeTo(null);
		setResizable(false);
		setContentPane(contentPane);

		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		JButton btnNewButton = new JButton("Cancel");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
				closed = true;
			}
		});
		panel.add(btnNewButton, BorderLayout.EAST);

		JButton btnNewButton_1 = new JButton("Select");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				InstallItem inst = ((InstallItem) comboBox.getSelectedItem());
				resultDir = inst.instdir;

				boolean coremod = false;
				if (file.getName().endsWith(".ccmf")) {
					coremod = true;
				}

				if (mf.supportedModLoaders.size() != 0) {
					if (mf.supportedModLoaders.containsKey(inst.installation.rootLoader)) {
						if (coremod)
							resultDir = new File(resultDir, inst.installation.getRootLoader().coreModInstallDir);
						else
							resultDir = new File(resultDir, inst.installation.getRootLoader().modInstallDir);
					} else {
						for (String key : mf.supportedModLoaders.keySet()) {
							if (inst.installation.hasLoader(key)) {
								if (coremod)
									resultDir = new File(resultDir, inst.installation.getLoader(key).coreModInstallDir);
								else
									resultDir = new File(resultDir, inst.installation.getLoader(key).modInstallDir);
								break;
							}
						}
					}
				} else {
					if (coremod)
						resultDir = new File(resultDir, inst.installation.getRootLoader().coreModInstallDir);
					else
						resultDir = new File(resultDir, inst.installation.getRootLoader().modInstallDir);
				}

				dispose();
				closed = true;
			}
		});
		panel.add(btnNewButton_1, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		contentPane.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(null);

		comboBox = new JComboBox<InstallItem>();
		comboBox.setBounds(56, 36, 515, 36);
		comboBox.setFont(new Font("serif", Font.PLAIN, 14));
		panel_1.add(comboBox);

		JLabel lblNewLabel = new JLabel("Select destination " + config.name.toUpperCase() + " installation...");
		lblNewLabel.setBounds(56, 18, 455, 15);
		panel_1.add(lblNewLabel);
	}
}
