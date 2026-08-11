package saccolink.gui;

import saccolink.model.AppUser;
import saccolink.service.UserService;
import saccolink.util.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

/**
 * Application login (multi-user simulation).
 * Validates credentials against APP_USER via SP_LOGIN and returns the
 * authenticated user (MEMBER or SACCO) on success.
 */
public class LoginDialog extends JDialog {

	private final JTextField username = new JTextField(20);
	private final JPasswordField password = new JPasswordField(20);
	private final JButton login = new JButton("Login");

	private AppUser loggedInUser;

	public LoginDialog(Frame owner) {
		super(owner, "SACCOLink - Login", true);
		setLayout(new BorderLayout(8, 8));

		add(buildHeader(), BorderLayout.NORTH);

		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 16, 4, 16));
		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(5, 5, 5, 5);
		g.anchor = GridBagConstraints.WEST;
		g.fill = GridBagConstraints.HORIZONTAL;
		g.weightx = 1;

		addRow(form, g, 0, "Username", username);
		addRow(form, g, 1, "Password", password);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton cancel = new JButton("Cancel");
		getRootPane().setDefaultButton(login);
		UiUtil.primary(login);
		UiUtil.secondary(cancel);
		buttons.add(login);
		buttons.add(cancel);

		login.addActionListener(e -> doLogin());
		password.addActionListener(e -> doLogin());
		cancel.addActionListener(e -> dispose());

		add(form, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);

		java.awt.EventQueue.invokeLater(() -> {
			toFront();
			requestFocus();
			username.requestFocusInWindow();
		});
	}

	private JPanel buildHeader() {
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(UiUtil.NAV_BG);
		header.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 2, 0, UiUtil.NAV_ACTIVE),
				BorderFactory.createEmptyBorder(14, 16, 14, 16)));

		JLabel title = new JLabel("SACCOLink");
		title.setForeground(java.awt.Color.WHITE);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

		JLabel sub = new JLabel("Credit Passport System - sign in");
		sub.setForeground(new java.awt.Color(0x9DB8B2));
		sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12f));

		JPanel titles = new JPanel();
		titles.setOpaque(false);
		titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
		titles.add(title);
		titles.add(sub);
		header.add(titles, BorderLayout.WEST);
		return header;
	}

	private void addRow(JPanel panel, GridBagConstraints g, int y, String label, JTextField field) {
		g.gridx = 0;
		g.gridy = y;
		g.gridwidth = 1;
		panel.add(new JLabel(label), g);
		g.gridx = 1;
		g.gridwidth = 2;
		panel.add(field, g);
	}

	private void doLogin() {
		String user = username.getText().trim();
		String pass = new String(password.getPassword());
		if (user.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Enter your username.",
					"Login", JOptionPane.ERROR_MESSAGE);
			return;
		}
		login.setEnabled(false);
		new Thread(() -> {
			try {
				AppUser u = UserService.login(user, pass);
				java.awt.EventQueue.invokeLater(() -> {
					login.setEnabled(true);
					if (u == null) {
						JOptionPane.showMessageDialog(this,
								"Invalid username or password.",
								"Login failed", JOptionPane.ERROR_MESSAGE);
						password.setText("");
					} else {
						loggedInUser = u;
						dispose();
					}
				});
			} catch (SQLException ex) {
				java.awt.EventQueue.invokeLater(() -> {
					login.setEnabled(true);
					JOptionPane.showMessageDialog(this,
							"Login error:\n" + ex.getMessage(),
							"Login failed", JOptionPane.ERROR_MESSAGE);
				});
			}
		}).start();
	}

	/** Blocks until the dialog is dismissed; the authenticated user, or null. */
	public AppUser showAndGetUser() {
		setVisible(true);
		return loggedInUser;
	}
}
