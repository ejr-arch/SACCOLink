package saccolink;

import saccolink.db.DBConnection;
import saccolink.gui.ConnectionDialog;
import saccolink.gui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

/**
 * SACCOLink GUI entry point.
 *
 * Usage:
 *   ./run.sh                      -> opens the Connect dialog first, then login
 *   ./run.sh --url jdbc:oracle:thin:@//localhost:1521/XEPDB1 --user SACCOLINK --pass secret
 *
 * The Connect dialog is always shown unless connection details are supplied
 * on the command line (or via the DB_HOST/DB_USER/... environment variables).
 */
public final class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Exception ignored) {
                // keep the default look and feel
            }
            UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            MainFrame frame = new MainFrame();
            frame.setVisible(true);

            boolean preConfigured = parseArgs(frame, args);
            if (!preConfigured && !frame.openConnectionDialog()) {
                frame.dispose();
                return;
            }
            if (!frame.showLoginDialog()) {
                frame.dispose();
            }
        });
    }

    /** Parses --url/--user/--pass (and host/port/service) command line options. */
    private static boolean parseArgs(MainFrame frame, String[] args) {
        String url = null, user = null, pass = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--url":
                    if (i + 1 < args.length) {
                        url = args[++i];
                    }
                    break;
                case "--user":
                    if (i + 1 < args.length) {
                        user = args[++i];
                    }
                    break;
                case "--pass":
                    if (i + 1 < args.length) {
                        pass = args[++i];
                    }
                    break;
                default:
                    break;
            }
        }
        if (url != null && user != null) {
            DBConnection.configureRaw(url, user, pass == null ? "" : pass);
            return true;
        }
        if (url != null || user != null) {
            JOptionPane.showMessageDialog(frame,
                    "--url and --user must be provided together.\n"
                    + "Opening the Connect dialog instead.",
                    "Connection options", JOptionPane.WARNING_MESSAGE);
        }
        return false;
    }
}
