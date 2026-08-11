package saccolink.util;

import saccolink.db.DBConnection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;

/** Small shared UI helpers used across the SACCOLink panels. */
public final class UiUtil {

    /** Sentinel option shown in filter combo boxes for "no filter". */
    public static final String ALL = "All records";

    // ------------------------------------------------------------------
    // Theme palette - deep teal green (SACCOLink brand)
    // ------------------------------------------------------------------

    /** Primary brand colour used for the sidebar, headers and actions. */
    public static final Color PRIMARY = new Color(0x00695C);
    /** Darker shade of the brand colour (nav background). */
    public static final Color NAV_BG = new Color(0x004D40);
    /** Hover shade for sidebar items. */
    public static final Color NAV_HOVER = new Color(0x00695C);
    /** Active page highlight in the sidebar. */
    public static final Color NAV_ACTIVE = new Color(0x2E7D32);
    /** Positive / success accent. */
    public static final Color ACCENT = new Color(0x2E7D32);
    /** Warning / attention accent (pending requests, stale rows). */
    public static final Color WARN = new Color(0xEF6C00);
    /** Error / danger accent. */
    public static final Color DANGER = new Color(0xC62828);

    /** Main window background (very light neutral). */
    public static final Color BG = new Color(0xF2F5F4);
    /** Card / panel surface. */
    public static final Color CARD_BG = Color.WHITE;
    /** Primary text. */
    public static final Color TEXT = new Color(0x212121);
    /** Muted / secondary text. */
    public static final Color TEXT_MUTED = new Color(0x616161);
    /** Hairline border colour. */
    public static final Color BORDER = new Color(0xD9DEDC);
    /** Table zebra striping colour. */
    public static final Color ROW_ALT = new Color(0xF4F8F7);
    /** Table selection highlight. */
    public static final Color SELECTION = new Color(0xCBE5DF);

    private UiUtil() {
    }

    /** Builds a titled, border-laid-out panel with a hairline border. */
    public static JPanel titled(String title) {
        JPanel p = new JPanel(new BorderLayout());
        javax.swing.border.TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER), title);
        b.setTitleFont(p.getFont().deriveFont(Font.BOLD, 13f));
        b.setTitleColor(PRIMARY);
        p.setBorder(BorderFactory.createCompoundBorder(b,
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        return p;
    }

    /** Shows an error dialog, unwrapping SQL exceptions into readable text. */
    public static void showError(Component parent, String title, Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            msg = e.toString();
        }
        showError(parent, title, msg);
    }

    public static void showError(Component parent, String title, String msg) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String title, String msg) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** True if the database is currently reachable. */
    public static boolean checkDb(Component parent) {
        try {
            DBConnection.testConnection();
            return true;
        } catch (SQLException e) {
            showError(parent, "Database unavailable",
                    "Cannot reach the database:\n" + e.getMessage());
            return false;
        }
    }

    /** Executes a Swing worker on the EDT, surfacing exceptions in a dialog. */
    public static void runAsync(Component parent, String errorTitle, Runnable task) {
        new Thread(() -> {
            try {
                task.run();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> showError(parent, errorTitle, e));
            }
        }).start();
    }

    /**
     * Creates a JTable that can be refreshed in place from a 2-D data matrix
     * using {@link #refreshTable}.
     */
    public static JTable table(Object[] columns) {
        JTable t = new JTable(new DefaultTableModel(columns, 0));
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.setRowHeight(28);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(SELECTION);
        t.setSelectionForeground(TEXT);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 34));
        h.setDefaultRenderer(new HeaderRenderer());

        t.setDefaultRenderer(Object.class, new StripedRenderer());
        return t;
    }

    public static void refreshTable(JTable t, Object[][] rows) {
        DefaultTableModel m = (DefaultTableModel) t.getModel();
        m.setRowCount(0);
        for (Object[] row : rows) {
            m.addRow(row);
        }
    }

    public static JScrollPane scroll(JTable t) {
        return new JScrollPane(t);
    }

    /** Renders a coloured, left-aligned column header. */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setOpaque(true);
            setBackground(PRIMARY);
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD, 13f));
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            setHorizontalAlignment(SwingConstants.LEFT);
        }
    }

    /** Zebra-stripes body rows while keeping the selected row highlighted. */
    private static final class StripedRenderer extends DefaultTableCellRenderer {
        StripedRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
            if (isSelected) {
                c.setBackground(SELECTION);
            } else {
                c.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
            }
            return c;
        }
    }

    /** Form row helper: label + field on a grid. */
    public static void addField(JPanel panel, GridBagConstraints g, int y,
                                String label, Component field) {
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 1;
        panel.add(new JLabel(label), g);
        g.gridx = 1;
        g.gridwidth = 2;
        panel.add(field, g);
    }

    /** A simple "hint" label used as page header text. */
    public static JLabel hint(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    /** A form with a flow layout of buttons at the bottom. */
    public static JPanel buttonRow(JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (JButton b : buttons) {
            row.add(b);
        }
        return row;
    }

    /** Standard GridBagConstraints used by every form panel. */
    public static GridBagConstraints grid() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }

    // ------------------------------------------------------------------
    // Button styling
    // ------------------------------------------------------------------

    /** Styles a button as the primary (filled, brand-coloured) action. */
    public static JButton primary(JButton b) {
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        return b;
    }

    /** Styles a button as a secondary (outlined, neutral) action. */
    public static JButton secondary(JButton b) {
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setFocusPainted(false);
        b.setBackground(CARD_BG);
        b.setForeground(TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        return b;
    }

    /** Styles a button as a flat sidebar navigation item (left-aligned). */
    public static JButton navItem(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 12));
        b.setForeground(new Color(0xDDE7E4));
        b.setBackground(NAV_BG);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText(text);
        return b;
    }

    /** Applies the hover / pressed visual feedback for sidebar items. */
    public static void styleNav(JButton b, boolean active) {
        b.setBackground(active ? NAV_ACTIVE : NAV_BG);
        b.setForeground(active ? Color.WHITE : new Color(0xDDE7E4));
        if (active) {
            b.setFont(b.getFont().deriveFont(Font.BOLD));
        }
    }

    // ------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------

    /** Converts a database SQLException into a readable line. */
    public static String sqlMessage(SQLException e) {
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        // strip raw ORA codes for readability, keep them as context
        return msg;
    }

    public static <E> void populateCombo(JComboBox<E> combo, List<E> items) {
        combo.removeAllItems();
        for (E item : items) {
            combo.addItem(item);
        }
    }
}
