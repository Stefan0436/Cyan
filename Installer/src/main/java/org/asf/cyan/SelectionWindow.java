package org.asf.cyan;

import java.awt.BorderLayout;
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

import org.asf.cyan.KickStartConfig.KickStartInstallation;
import org.asf.cyan.api.versioning.Version;

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
	private File result;
	private File file;
	private JComboBox<InstallItem> comboBox;

	private ProjectConfig config = new ProjectConfig();

	private class InstallItem {
		public String dsp;
		public File dir;

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

		CyanModfileManifest mf = getModManifest(file);
		KickStartConfig conf = new KickStartConfig().readAll(Files.readString(installs.toPath()));
		for (KickStartInstallation install : conf.installations) {
			if (checkInstall(mf, install)) {
				File data = new File(install.cyanData);
				InstallItem inst = new InstallItem();
				inst.dsp = "[" + install.platform + "] [" + install.side + "] " + data.getParentFile().getName() + " ("
						+ install.gameVersion + ", " + install.loaderVersion + ")";
				inst.dir = new File(install.cyanData);
				if (inst.dir.exists())
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
			result = comboBox.getItemAt(0).dir;
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

		return frame.result;
	}

	public boolean checkInstall(CyanModfileManifest manifest, KickStartInstallation install) {
		if (manifest.gameVersionRegex != null && !install.gameVersion.matches(manifest.gameVersionRegex))
			return false;
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
				result = ((InstallItem) comboBox.getSelectedItem()).dir;
				dispose();
				closed = true;
			}
		});
		panel.add(btnNewButton_1, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		contentPane.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(null);

		comboBox = new JComboBox<InstallItem>();
		comboBox.setBounds(86, 46, 455, 24);
		panel_1.add(comboBox);

		JLabel lblNewLabel = new JLabel("Select destination " + config.name.toUpperCase() + " installation...");
		lblNewLabel.setBounds(86, 30, 455, 15);
		panel_1.add(lblNewLabel);
	}
}
