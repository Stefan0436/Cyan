package org.asf.cyan.minecraft.toolkits.mtk.auth.windowed;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.border.BevelBorder;

import org.asf.cyan.minecraft.toolkits.mtk.auth.AuthenticationInfo;
import org.asf.cyan.minecraft.toolkits.mtk.auth.YggdrasilAuthentication;

import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;
import java.awt.FlowLayout;

public class YggdrasilAuthenticationWindow {

	private JDialog frame;
	private JTextField textField;
	private JPasswordField textField_1;
	private JLabel lblNewLabel_2;
	private AuthenticationInfo info = null;

	public AuthenticationInfo getAccount() {
		return info;
	}

	public YggdrasilAuthenticationWindow() {
		this("");
	}

	public YggdrasilAuthenticationWindow(String startingUsername) {
		this(startingUsername, "Press enter to log in... You will be prompted for a password if necessary...");
	}

	public YggdrasilAuthenticationWindow(String startingUsername, String startingError) {
		try {
			EventQueue.invokeAndWait(new Runnable() {
				public void run() {
					initialize();
					textField.setText(startingUsername);
					lblNewLabel_2.setText(startingError);
					frame.setModal(true);
					frame.setVisible(true);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	private void initialize() {
		frame = new JDialog();
		frame.setBounds(100, 100, 671, 336);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setTitle("Yggdrasil simple login");

		JLabel lblMinecraft = new JLabel("Minecraft Installation Toolkit - Yggdrasil Login");
		lblMinecraft.setPreferredSize(new Dimension(326, 60));
		lblMinecraft.setFont(new Font("Dialog", Font.BOLD, 20));
		lblMinecraft.setHorizontalAlignment(SwingConstants.CENTER);
		frame.getContentPane().add(lblMinecraft, BorderLayout.NORTH);

		JPanel panel = new JPanel();
		panel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel_2 = new JPanel();
		panel_2.setPreferredSize(new Dimension(600, 195));
		panel_2.setLayout(null);

		textField = new JTextField();
		textField.setBounds(0, 69, 600, 19);
		panel_2.add(textField);
		textField.setColumns(10);

		JLabel lblNewLabel = new JLabel("Minecraft Login Email Address (or username for legacy accounts)");
		lblNewLabel.setBounds(0, 48, 600, 15);
		panel_2.add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("Minecraft Password");
		lblNewLabel_1.setBounds(0, 94, 600, 15);
		panel_2.add(lblNewLabel_1);

		textField_1 = new JPasswordField();
		textField_1.setBounds(0, 115, 600, 19);
		panel_2.add(textField_1);

		textField.setBounds(0, (int) (panel_2.getPreferredSize().getHeight() / 2) - (textField.getHeight() / 2), 600,
				19);
		lblNewLabel.setBounds(0, textField.getY() - 20, 600, 15);
		lblNewLabel_1.setVisible(false);
		textField_1.setVisible(false);

		textField_1.setColumns(10);

		lblNewLabel_2 = new JLabel("Press enter to log in... You will be prompted for a password if necessary...");
		lblNewLabel_2.setBounds(0, 146, 600, 42);
		panel_2.add(lblNewLabel_2);
		panel.add(panel_2);

		JPanel panel_1 = new JPanel();
		frame.getContentPane().add(panel_1, BorderLayout.SOUTH);

		JButton btnNewButton_1 = new JButton("Cancel");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
			}
		});
		panel_1.add(btnNewButton_1);

		JButton btnNewButton = new JButton("Login");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				info = null;
				lblNewLabel_2.setText("");
				if (!textField.getText().isEmpty() && (textField.hasFocus()
						|| (btnNewButton.hasFocus() && textField_1.getPassword().length == 0))) {
					if (YggdrasilAuthentication.hasBeenSaved(textField.getText())) {
						try {
							info = YggdrasilAuthentication.authenticate(textField.getText());
							frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
						} catch (IOException e) {
							lblNewLabel_2.setText(e.getMessage());
							textField_1.setText("");

							lblNewLabel.setBounds(0, 48, 600, 15);
							textField.setBounds(0, 69, 600, 19);
							lblNewLabel_1.setVisible(true);
							textField_1.setVisible(true);

							textField_1.grabFocus();
						}
					} else {

						lblNewLabel.setBounds(0, 48, 600, 15);
						textField.setBounds(0, 69, 600, 19);
						lblNewLabel_1.setVisible(true);
						textField_1.setVisible(true);

						textField_1.grabFocus();
					}
				} else if (textField.getText().isEmpty()) {

					textField.setBounds(0,
							(int) (panel_2.getPreferredSize().getHeight() / 2) - (textField.getHeight() / 2), 600, 19);
					lblNewLabel.setBounds(0, textField.getY() - 20, 600, 15);
					lblNewLabel_1.setVisible(false);
					textField_1.setVisible(false);

					textField_1.setText("Press enter to log in... You will be prompted for a password if necessary...");
				} else if (textField_1.hasFocus() || btnNewButton.hasFocus()) {
					try {
						info = YggdrasilAuthentication.authenticate(textField.getText(),
								new String(textField_1.getPassword()));
						frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
					} catch (IOException e) {
						lblNewLabel_2.setText(e.getMessage());
						textField_1.setText("");

						textField.setBounds(0,
								(int) (panel_2.getPreferredSize().getHeight() / 2) - (textField.getHeight() / 2), 600,
								19);
						lblNewLabel.setBounds(0, textField.getY() - 20, 600, 15);
						lblNewLabel_1.setVisible(false);
						textField_1.setVisible(false);

						textField.grabFocus();
					}
				}
			}
		});
		panel_1.add(btnNewButton);

		frame.getRootPane().setDefaultButton(btnNewButton);
	}
}
