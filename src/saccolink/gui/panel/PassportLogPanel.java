package saccolink.gui.panel;

import saccolink.dao.PassportDAO;
import saccolink.gui.Refreshable;
import saccolink.model.PassportDetail;
import saccolink.session.Session;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Page 10 - Passport Log (interactive report over V_PASSPORT_DETAIL).
 * Filter by status / token, copy a selected token, revoke an active
 * passport, expire overdue ones.
 */
public class PassportLogPanel extends JPanel implements Refreshable {

    private static final String[] COLS =
            {"ID", "Token", "Status", "Member", "Score", "Band", "Views", "Generated", "Expires"};

    private final JTable table = UiUtil.table(COLS);
    private final JComboBox<String> statusFilter =
            new JComboBox<>(new String[]{UiUtil.ALL, "ACTIVE", "EXPIRED", "REVOKED"});
    private final JTextField tokenFilter = new JTextField(14);
    private final JButton refreshBtn = new JButton("Refresh");
    private final JButton searchBtn = new JButton("Search");
    private final JButton copyBtn = new JButton("Copy Selected Token");
    private final JButton revokeBtn = new JButton("Revoke Selected");
    private final JButton expireBtn = new JButton("Expire Overdue");

    public PassportLogPanel() {
        super(new BorderLayout(8, 8));

        JPanel filters = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        filters.add(new javax.swing.JLabel("Status:"));
        filters.add(statusFilter);
        filters.add(new javax.swing.JLabel("Token contains:"));
        filters.add(tokenFilter);
        filters.add(searchBtn);
        filters.add(refreshBtn);

        statusFilter.addActionListener(e -> refresh());
        tokenFilter.addActionListener(e -> refresh());
        searchBtn.addActionListener(e -> refresh());
        refreshBtn.addActionListener(e -> refresh());
        copyBtn.addActionListener(e -> copySelectedToken());
        revokeBtn.addActionListener(e -> revokeSelected());
        expireBtn.addActionListener(e -> expireOverdue());

        copyBtn.setEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e ->
                copyBtn.setEnabled(!e.getValueIsAdjusting() && table.getSelectedRow() >= 0));

        add(filters, BorderLayout.NORTH);
        add(UiUtil.scroll(table), BorderLayout.CENTER);
        add(UiUtil.buttonRow(expireBtn, copyBtn, revokeBtn), BorderLayout.SOUTH);
    }

    /** Copies the QR token of the selected passport row to the clipboard. */
    private void copySelectedToken() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiUtil.showInfo(this, "Nothing selected", "Select a passport row to copy its token.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String token = (String) table.getModel().getValueAt(modelRow, 1);
        String member = (String) table.getModel().getValueAt(modelRow, 3);
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(token), null);
        UiUtil.showInfo(this, "Token copied",
                "QR token for " + member + " copied to the clipboard.");
    }

    private void revokeSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UiUtil.showInfo(this, "Nothing selected", "Select a passport row to revoke.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        String token = (String) table.getModel().getValueAt(modelRow, 1);
        String status = (String) table.getModel().getValueAt(modelRow, 2);
        if (!"ACTIVE".equals(status)) {
            UiUtil.showInfo(this, "Cannot revoke",
                    "Only ACTIVE passports can be revoked (this one is " + status + ").");
            return;
        }
        int choice = javax.swing.JOptionPane.showConfirmDialog(this,
                "Revoke passport ending ..." + token.substring(Math.max(0, token.length() - 6)) + "?",
                "Revoke passport", javax.swing.JOptionPane.YES_NO_OPTION);
        if (choice != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        try {
            PassportDAO.revoke(token);
            UiUtil.showInfo(this, "Revoked", "Passport is now REVOKED.");
            refresh();
        } catch (SQLException ex) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(ex));
        }
    }

    private void expireOverdue() {
        try {
            int n = PassportDAO.expirePassports();
            UiUtil.showInfo(this, "Done", n + " passport(s) marked EXPIRED.");
            refresh();
        } catch (SQLException ex) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(ex));
        }
    }

    @Override
    public void refresh() {
        revokeBtn.setVisible(Session.isSacco());
        expireBtn.setVisible(Session.isSacco());
        String status = (String) statusFilter.getSelectedItem();
        if (UiUtil.ALL.equals(status)) {
            status = null;
        }
        String token = tokenFilter.getText().trim();
        try {
            List<PassportDetail> rows = PassportDAO.findAll(status, token);
            Object[][] data = new Object[rows.size()][];
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (int i = 0; i < rows.size(); i++) {
                PassportDetail p = rows.get(i);
                data[i] = new Object[]{
                        p.getPassportId(),
                        p.getQrToken(),
                        p.getPassportStatus(),
                        p.getMemberName(),
                        String.format("%,.2f", p.getScoreValue()),
                        p.getScoreBand(),
                        p.getViewCount(),
                        sdf.format(p.getGeneratedAt()),
                        sdf.format(p.getExpiresAt())
                };
            }
            UiUtil.refreshTable(table, data);
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }
}
