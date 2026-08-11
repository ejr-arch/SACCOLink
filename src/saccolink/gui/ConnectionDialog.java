package saccolink.gui;

import saccolink.db.DBConnection;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;

/**
 * Startup dialog that captures Oracle connection details and tests them
 * before the main window is shown.
 */
public class ConnectionDialog extends JDialog {

    private final JTextField host = new JTextField("localhost", 20);
    private final JTextField port = new JTextField("1521", 8);
    private final JTextField service = new JTextField("XEPDB1", 20);
    private final JTextField user = new JTextField("SACCOLINK", 20);
    private final JPasswordField pass = new JPasswordField(20);
    private final JCheckBox remember = new JCheckBox("Remember for this session");

    private boolean connected;

    public ConnectionDialog(Frame owner) {
        super(owner, "Connect to Oracle Database", true);
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        addRow(form, g, 0, "Host", host);
        addRow(form, g, 1, "Port", port);
        addRow(form, g, 2, "Service name", service);
        addRow(form, g, 3, "Username", user);
        addRow(form, g, 4, "Password", pass);
        g.gridx = 0; g.gridwidth = 2; g.gridy = 5;
        form.add(remember, g);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton test = new JButton("Test Connection");
        JButton connect = new JButton("Connect");
        JButton cancel = new JButton("Cancel");
        getRootPane().setDefaultButton(connect);
        buttons.add(test);
        buttons.add(connect);
        buttons.add(cancel);

        test.addActionListener(e -> testNow(false));
        connect.addActionListener(e -> {
            if (testNow(true)) {
                dispose();
            }
        });
        cancel.addActionListener(e -> dispose());

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private void addRow(JPanel panel, GridBagConstraints g, int y, String label, JTextField field) {
        g.gridx = 0; g.gridy = y; g.gridwidth = 1;
        panel.add(new JLabel(label), g);
        g.gridx = 1; g.gridwidth = 2;
        panel.add(field, g);
    }

    private boolean testNow(boolean showSuccess) {
        try {
            int p = Integer.parseInt(port.getText().trim());
            DBConnection.configure(host.getText().trim(), p, service.getText().trim(),
                    user.getText().trim(), new String(pass.getPassword()));
            DBConnection.testConnection();
            connected = true;
            if (showSuccess) {
                JOptionPane.showMessageDialog(this,
                        "Connected successfully.", "SACCOLink", JOptionPane.INFORMATION_MESSAGE);
            }
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port must be a number.",
                    "Invalid port", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (SQLException e) {
            connected = false;
            JOptionPane.showMessageDialog(this,
                    "Connection failed:\n" + e.getMessage(),
                    "Connection failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Blocks until the dialog is dismissed; true if a connection was made. */
    public boolean showAndGetConnected() {
        setVisible(true);
        return connected;
    }
}
