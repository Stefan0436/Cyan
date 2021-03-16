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
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

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
		this(startingUsername, "");
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

	private void initialize() {
		frame = new JDialog();
		frame.setBounds(100, 100, 622, 399);
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

		JLabel lblNewLabel = new JLabel("Minecraft Username");

		textField = new JTextField();
		textField.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("Minecraft Password");

		textField_1 = new JPasswordField();
		textField_1.setEnabled(false);
		textField_1.setColumns(10);

		lblNewLabel_2 = new JLabel("");

		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(gl_panel.createParallelGroup(Alignment.LEADING).addGroup(gl_panel
				.createSequentialGroup().addContainerGap()
				.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addComponent(textField, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 594, Short.MAX_VALUE)
						.addComponent(lblNewLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 594, Short.MAX_VALUE)
						.addGroup(Alignment.TRAILING,
								gl_panel.createParallelGroup(Alignment.LEADING)
										.addComponent(lblNewLabel_1, GroupLayout.PREFERRED_SIZE, 594,
												GroupLayout.PREFERRED_SIZE)
										.addComponent(textField_1, GroupLayout.PREFERRED_SIZE, 594,
												GroupLayout.PREFERRED_SIZE))
						.addComponent(lblNewLabel_2))
				.addContainerGap()));
		gl_panel.setVerticalGroup(gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup().addGap(41).addComponent(lblNewLabel)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(textField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(lblNewLabel_1).addGap(6)
						.addComponent(textField_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addGap(33).addComponent(lblNewLabel_2).addContainerGap(90, Short.MAX_VALUE)));
		panel.setLayout(gl_panel);

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
				if (!textField.getText().isEmpty() && (textField.hasFocus() || (btnNewButton.hasFocus() && textField_1.getPassword().length == 0))) {
					if (YggdrasilAuthentication.hasBeenSaved(textField.getText())) {
						try {
							info = YggdrasilAuthentication.authenticate(textField.getText());
							frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
						} catch (IOException e) {
							lblNewLabel_2.setText(e.getMessage());
							textField_1.setText("");
							textField_1.setEnabled(true);
							textField_1.grabFocus();
						}
					} else {
						textField_1.setEnabled(true);
						textField_1.grabFocus();
					}
				} else if (textField.getText().isEmpty()) {
					textField_1.setEnabled(false);
					textField_1.setText("");
				} else if (textField_1.hasFocus() || btnNewButton.hasFocus()) {
					try {
						info = YggdrasilAuthentication.authenticate(textField.getText(),
								new String(textField_1.getPassword()));
						frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
					} catch (IOException e) {
						lblNewLabel_2.setText(e.getMessage());
						textField_1.setText("");
						textField.grabFocus();
					}
				}
			}
		});
		panel_1.add(btnNewButton);

		frame.getRootPane().setDefaultButton(btnNewButton);
	}
}
