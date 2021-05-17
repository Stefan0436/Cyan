package org.asf.cyan;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JTextArea;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.awt.event.ActionEvent;

public class ModInstaller extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	public File mod;
	public File cyanDataDir;

	private JTextArea textArea;
	private JLabel lblNewLabel;
	private JLabel lblNewLabel_1;
	private CyanModfileManifest mf;

	/**
	 * Create the frame.
	 */
	public ModInstaller() {
		setTitle("KickStart Mod Installer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 704, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(10, 70));
		contentPane.add(panel, BorderLayout.NORTH);
		panel.setLayout(null);

		lblNewLabel = new JLabel("New label");
		lblNewLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
		lblNewLabel.setBounds(12, 12, 670, 24);
		panel.add(lblNewLabel);

		lblNewLabel_1 = new JLabel("New label");
		lblNewLabel_1.setFont(new Font("SansSerif", Font.BOLD, 12));
		lblNewLabel_1.setBounds(12, 45, 670, 15);
		panel.add(lblNewLabel_1);

		JPanel panel_1 = new JPanel();
		contentPane.add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new BorderLayout(0, 0));

		JButton btnNewButton = new JButton("Cancel");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		panel_1.add(btnNewButton, BorderLayout.EAST);

		JButton btnNewButton_1 = new JButton("Confirm installation");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!mod.getName().endsWith(".ccmf") && !mod.getName().endsWith(".cmf")) {
					JOptionPane.showMessageDialog(ModInstaller.this, "Broken mod file, not a cmf or ccmf.",
							"Broken mod", JOptionPane.WARNING_MESSAGE);
					dispose();
					return;
				}

				File dest = new File(cyanDataDir,
						mod.getName().endsWith(".cmf") ? "mods" : mod.getName().endsWith(".ccmf") ? "coremods" : "");
				File modDest = new File(dest,
						mf.modGroup + "." + mf.modId + mod.getName().substring(mod.getName().lastIndexOf(".")));
				if (modDest.exists())
					modDest.delete();

				if (!dest.exists())
					dest.mkdirs();
				try {
					Files.copy(mod.toPath(), modDest.toPath());
					JOptionPane.showMessageDialog(ModInstaller.this, "Installation completed.", "Install successful",
							JOptionPane.INFORMATION_MESSAGE);
					dispose();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(ModInstaller.this, "Broken mod file, copy failed.", "Broken mod",
							JOptionPane.WARNING_MESSAGE);
					dispose();
				}
			}
		});
		panel_1.add(btnNewButton_1, BorderLayout.CENTER);

		textArea = new JTextArea();
		textArea.setEditable(false);
		contentPane.add(textArea, BorderLayout.CENTER);
		setLocationRelativeTo(null);
	}

	public void load() throws IOException {
		mf = SelectionWindow.getModManifest(mod);
		textArea.setText((mf.fallbackDescription == null ? "No description" : mf.fallbackDescription.trim())
				+ "\n\nThis mod will be installed in:\n" + cyanDataDir.getCanonicalPath());
		lblNewLabel.setText(mf.displayName);
		lblNewLabel_1.setText(mf.modGroup + ":" + mf.modId + " V" + mf.version);
	}

}
