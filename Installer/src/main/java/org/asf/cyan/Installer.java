package org.asf.cyan;

import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import java.awt.Dimension;
import javax.swing.SwingConstants;

public class Installer {

	private JFrame frmCyanInstaller;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Installer window = new Installer();
					window.frmCyanInstaller.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Installer() throws IOException {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() throws IOException {
		frmCyanInstaller = new JFrame();
		frmCyanInstaller.setTitle("Installer");
		frmCyanInstaller.setResizable(false);
		frmCyanInstaller.setBounds(100, 100, 640, 428);
		frmCyanInstaller.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmCyanInstaller.setLocationRelativeTo(null);

		ProjectConfig project = new ProjectConfig();
		frmCyanInstaller.setTitle(project.name + " Installer");

		JPanel panel = new JPanel();
		frmCyanInstaller.getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel = new JLabel(" " + project.name + " " + project.version);
		lblNewLabel.setVerticalAlignment(SwingConstants.TOP);
		lblNewLabel.setPreferredSize(new Dimension(200, 18));
		panel.add(lblNewLabel, BorderLayout.WEST);
	}

}
