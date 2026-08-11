package saccolink.gui.panel;

import saccolink.dao.MemberDAO;
import saccolink.gui.MainFrame;
import saccolink.gui.Refreshable;
import saccolink.model.Member;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Page 2 - Member List (interactive report over MEMBER).
 * The filter bar above the table (search text + consent combo) narrows the
 * displayed records; double-click a row to edit it on Page 1.
 */
public class MemberListPanel extends JPanel implements Refreshable {

    private static final String[] COLS =
            {"ID", "NIN", "Full Name", "Phone", "District", "Consent", "Registered"};

    private final JTable table = UiUtil.table(COLS);
    private final JTextField search = new JTextField(20);
    private final JComboBox<String> consentFilter =
            new JComboBox<>(new String[]{UiUtil.ALL, "Consent: Y", "Consent: N"});
    private final MainFrame frame;

    public MemberListPanel(MainFrame frame) {
        super(new BorderLayout(8, 8));
        this.frame = frame;

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> refresh());
        JButton editBtn = new JButton("Edit Selected");
        editBtn.addActionListener(e -> openSelected());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new javax.swing.JLabel("Search (NIN/name/phone/district):"));
        filters.add(search);
        filters.add(searchBtn);
        filters.add(new javax.swing.JLabel("Consent:"));
        filters.add(consentFilter);

        search.addActionListener(e -> refresh());
        consentFilter.addActionListener(e -> refresh());

        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                editBtn.setEnabled(true);
            }
        });

        add(filters, BorderLayout.NORTH);
        add(UiUtil.scroll(table), BorderLayout.CENTER);
        add(UiUtil.buttonRow(editBtn, refreshBtn), BorderLayout.SOUTH);
    }

    private void openSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        long id = (long) table.getModel().getValueAt(modelRow, 0);
        try {
            Member m = MemberDAO.findById(id);
            if (m != null) {
                frame.showMemberRegistration(m);
            }
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }

    @Override
    public void refresh() {
        try {
            List<Member> all = MemberDAO.findAll();
            String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String consentSel = (String) consentFilter.getSelectedItem();

            List<Member> filtered = new ArrayList<>();
            for (Member m : all) {
                if (!q.isEmpty() && !matches(m, q)) {
                    continue;
                }
                if ("Consent: Y".equals(consentSel) && !m.isConsentGiven()) {
                    continue;
                }
                if ("Consent: N".equals(consentSel) && m.isConsentGiven()) {
                    continue;
                }
                filtered.add(m);
            }

            Object[][] rows = new Object[filtered.size()][];
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (int i = 0; i < filtered.size(); i++) {
                Member m = filtered.get(i);
                rows[i] = new Object[]{
                        m.getMemberId(),
                        m.getNin(),
                        m.getFullName(),
                        m.getPhoneNumber(),
                        m.getDistrict(),
                        m.isConsentGiven() ? "Y" : "N",
                        m.getCreatedAt() == null ? "" : sdf.format(m.getCreatedAt())
                };
            }
            UiUtil.refreshTable(table, rows);
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }

    private static boolean matches(Member m, String q) {
        return (m.getNin() != null && m.getNin().toLowerCase().contains(q))
                || (m.getFullName() != null && m.getFullName().toLowerCase().contains(q))
                || (m.getPhoneNumber() != null && m.getPhoneNumber().toLowerCase().contains(q))
                || (m.getDistrict() != null && m.getDistrict().toLowerCase().contains(q));
    }
}
