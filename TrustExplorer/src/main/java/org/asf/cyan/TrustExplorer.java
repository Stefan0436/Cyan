package org.asf.cyan;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.awt.event.ActionEvent;
import javax.swing.JProgressBar;

public class TrustExplorer {

	private JFrame frmTrustExplorerAlpha;
	private JTextField txtCtcoutput;
	private JProgressBar progressBar;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TrustExplorer window = new TrustExplorer();
					window.frmTrustExplorerAlpha.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public TrustExplorer() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmTrustExplorerAlpha = new JFrame();
		frmTrustExplorerAlpha.setResizable(false);
		frmTrustExplorerAlpha.setTitle("Trust Explorer Alpha");
		frmTrustExplorerAlpha.setBounds(100, 100, 470, 133);
		frmTrustExplorerAlpha.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JButton btnNewButton = new JButton("Import CTC File");
		frmTrustExplorerAlpha.getContentPane().add(btnNewButton, BorderLayout.WEST);

		JButton btnNewButton_1 = new JButton("Export CTC file");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File input = new File(txtCtcoutput.getText());
				progressBar.setMaximum(100);
				progressBar.setValue(0);
				btnNewButton.setEnabled(false);
				btnNewButton_1.setEnabled(false);
				new Thread(() -> {
					File output = new File(txtCtcoutput.getText() + "/ctc.ctc");
					try {
						CtcUtil.pack(input, output, (num) -> progressBar.setMaximum(num),
								(num) -> progressBar.setValue(num));
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(frmTrustExplorerAlpha, "Failed to pack files", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
					btnNewButton.setEnabled(true);
					btnNewButton_1.setEnabled(true);
				}).start();
			}
		});
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
				fileChooser.setDialogTitle("Choose CTC file to extract");
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Cyan Trust Container files", "ctc"));
				if (fileChooser.showOpenDialog(frmTrustExplorerAlpha) == JFileChooser.APPROVE_OPTION) {
					File input = fileChooser.getSelectedFile();
					progressBar.setMaximum(100);
					progressBar.setValue(0);
					btnNewButton.setEnabled(false);
					btnNewButton_1.setEnabled(false);
					new Thread(() -> {
						File outputDir = new File(txtCtcoutput.getText());
						try {
							CtcUtil.unpack(input, outputDir, (num) -> progressBar.setMaximum(num),
									(num) -> progressBar.setValue(num));
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(frmTrustExplorerAlpha, "Failed to unpack files", "Error",
									JOptionPane.ERROR_MESSAGE);
						}
						btnNewButton.setEnabled(true);
						btnNewButton_1.setEnabled(true);
					}).start();
				}
			}
		});
		frmTrustExplorerAlpha.getContentPane().add(btnNewButton_1, BorderLayout.CENTER);

		txtCtcoutput = new JTextField();
		txtCtcoutput.setText("./ctc-output/");
		frmTrustExplorerAlpha.getContentPane().add(txtCtcoutput, BorderLayout.NORTH);
		txtCtcoutput.setColumns(10);

		progressBar = new JProgressBar();
		frmTrustExplorerAlpha.getContentPane().add(progressBar, BorderLayout.SOUTH);
	}

}
