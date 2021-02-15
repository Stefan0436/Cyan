package org.asf.cyan;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

class DownloadWindow {

	private JFrame frmCyanloaderPreparing;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DownloadWindow window = new DownloadWindow();
					window.frmCyanloaderPreparing.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public DownloadWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmCyanloaderPreparing = new JFrame();
		frmCyanloaderPreparing.setResizable(false);
		frmCyanloaderPreparing.setAutoRequestFocus(false);
		frmCyanloaderPreparing.setTitle("CyanLoader - Preparing to launch");
		frmCyanloaderPreparing.setBounds(100, 100, 464, 130);
		frmCyanloaderPreparing.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel panel = new JPanel();
		frmCyanloaderPreparing.getContentPane().add(panel, BorderLayout.CENTER);

		JLabel lblNewLabel = new JLabel("Cyan is preparing to launch for the first time...");
		panel.add(lblNewLabel);
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);

		JLabel lblDependingOnYour = new JLabel("Depending on your network speed, this might take a while...");
		panel.add(lblDependingOnYour);

		JPanel panel_1 = new JPanel();
		frmCyanloaderPreparing.getContentPane().add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new BorderLayout(0, 0));

		JProgressBar progressBar = new JProgressBar();
		panel_1.add(progressBar);

		JLabel lblNewLabel_1 = new JLabel("Progress: ");
		panel_1.add(lblNewLabel_1, BorderLayout.NORTH);
	}

}
