package saccolink.gui.panel;

import saccolink.dao.LoanDAO;
import saccolink.dao.MemberDAO;
import saccolink.gui.MainFrame;
import saccolink.gui.Refreshable;
import saccolink.model.LoanRecord;
import saccolink.model.Member;
import saccolink.session.Session;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Page 4 - Loan List (interactive report over LOAN_RECORD).
 * Filter by member and status; double-click a row to edit on Page 3.
 */
public class LoanListPanel extends JPanel implements Refreshable {

    private static final String[] COLS =
            {"Loan ID", "Member", "Amount (UGX)", "Disbursed", "Repaid", "Status"};

    private final JTable table = UiUtil.table(COLS);
    private final JComboBox<Object> memberFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter =
            new JComboBox<>(new String[]{UiUtil.ALL, "ACTIVE", "REPAID", "DEFAULTED"});
    private final JTextField search = new JTextField(16);
    private final MainFrame frame;
    private final JButton editBtn = new JButton("Edit Selected");

    public LoanListPanel(MainFrame frame) {
        super(new BorderLayout(8, 8));
        this.frame = frame;

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> refresh());

        editBtn.addActionListener(e -> openSelected());

        JPanel filters = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        filters.add(new javax.swing.JLabel("Member:"));
        filters.add(memberFilter);
        filters.add(new javax.swing.JLabel("Status:"));
        filters.add(statusFilter);
        filters.add(new javax.swing.JLabel("Search (name/amount):"));
        filters.add(search);
        filters.add(searchBtn);
        memberFilter.addItem(UiUtil.ALL);
        statusFilter.addActionListener(e -> refresh());
        memberFilter.addActionListener(e -> refresh());
        search.addActionListener(e -> refresh());

        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e ->
                editBtn.setEnabled(!e.getValueIsAdjusting() && table.getSelectedRow() >= 0));

        add(filters, BorderLayout.NORTH);
        add(UiUtil.scroll(table), BorderLayout.CENTER);
        add(UiUtil.buttonRow(editBtn, refreshBtn), BorderLayout.SOUTH);
    }

    private void openSelected() {
        if (!Session.isSacco()) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        long id = (long) table.getModel().getValueAt(modelRow, 0);
        List<LoanRecord> loans;
        try {
            loans = LoanDAO.findAll(null, null);
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
            return;
        }
        for (LoanRecord loan : loans) {
            if (loan.getLoanId() == id) {
                frame.showLoanEntry(loan);
                return;
            }
        }
    }

    @Override
    public void refresh() {
        editBtn.setVisible(Session.isSacco());
        loadMemberFilter();
        Member member = selectedMember();
        String status = (String) statusFilter.getSelectedItem();
        if (UiUtil.ALL.equals(status)) {
            status = null;
        }
        try {
            List<LoanRecord> loans = LoanDAO.findAll(
                    member == null ? null : member.getMemberId(), status);
            String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            List<LoanRecord> filtered = loans;
            if (!q.isEmpty()) {
                filtered = new ArrayList<>();
                for (LoanRecord l : loans) {
                    if (loanMatches(l, q)) {
                        filtered.add(l);
                    }
                }
            }
            Object[][] rows = new Object[filtered.size()][];
            for (int i = 0; i < filtered.size(); i++) {
                LoanRecord l = filtered.get(i);
                rows[i] = new Object[]{
                        l.getLoanId(),
                        l.getMemberName(),
                        String.format("%,.0f", l.getLoanAmount()),
                        l.getDisbursementDate() == null ? "" : l.getDisbursementDate().toString(),
                        l.getRepaymentDate() == null ? "" : l.getRepaymentDate().toString(),
                        l.getLoanStatus()
                };
            }
            UiUtil.refreshTable(table, rows);
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }

    private static boolean loanMatches(LoanRecord l, String q) {
        return (l.getMemberName() != null && l.getMemberName().toLowerCase().contains(q))
                || String.format("%,.0f", l.getLoanAmount()).contains(q)
                || String.valueOf(l.getLoanAmount()).contains(q)
                || (l.getLoanStatus() != null && l.getLoanStatus().toLowerCase().contains(q));
    }

    private void loadMemberFilter() {
        Long keep = selectedMemberId();
        memberFilter.removeAllItems();
        memberFilter.addItem(UiUtil.ALL);
        try {
            List<Member> members = Session.isMember()
                    ? membersForSelf()
                    : MemberDAO.findAll();
            for (Member m : members) {
                memberFilter.addItem(m);
                if (m.getMemberId().equals(keep)) {
                    memberFilter.setSelectedItem(m);
                }
            }
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }

    /** A MEMBER may only ever filter on their own account. */
    private List<Member> membersForSelf() {
        try {
            Member self = MemberDAO.findById(Session.memberId());
            if (self != null) {
                List<Member> one = new ArrayList<>();
                one.add(self);
                return one;
            }
        } catch (SQLException ignored) {
            // fall through to an empty list
        }
        return new ArrayList<>();
    }

    /** The currently selected member, or null when "All records" is chosen. */
    private Member selectedMember() {
        Object sel = memberFilter.getSelectedItem();
        return sel instanceof Member ? (Member) sel : null;
    }

    /** The selected member id, or null for "All records". */
    private Long selectedMemberId() {
        Member m = selectedMember();
        return m == null ? null : m.getMemberId();
    }
}
