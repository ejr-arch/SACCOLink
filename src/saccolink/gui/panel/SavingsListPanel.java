package saccolink.gui.panel;

import saccolink.dao.MemberDAO;
import saccolink.dao.SavingsDAO;
import saccolink.gui.Refreshable;
import saccolink.model.Member;
import saccolink.model.SavingsReport;
import saccolink.session.Session;
import saccolink.util.UiUtil;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Page 6 - Savings Report (aggregate, not a transaction table).
 * One row per member: months contributed, total saved, average per month,
 * and first/last contribution month, with an overall TOTAL SAVINGS summary.
 */
public class SavingsListPanel extends JPanel implements Refreshable {

    private static final String[] COLS =
            {"Member", "Contributions", "Total Saved (UGX)", "Avg / Month (UGX)",
             "First Month", "Last Month"};

    private final JTable table = UiUtil.table(COLS);
    private final JComboBox<Object> memberFilter = new JComboBox<>();
    private final JLabel totalLabel = new JLabel(" ");

    public SavingsListPanel() {
        super(new BorderLayout(8, 8));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new javax.swing.JLabel("Member:"));
        filters.add(memberFilter);
        memberFilter.addItem(UiUtil.ALL);
        memberFilter.addActionListener(e -> refresh());

        totalLabel.setFont(totalLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        totalLabel.setForeground(new Color(0x1B5E20));
        totalLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 12, 6, 12));

        JPanel north = new JPanel(new BorderLayout());
        north.add(filters, BorderLayout.NORTH);
        north.add(totalLabel, BorderLayout.SOUTH);

        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);

        add(north, BorderLayout.NORTH);
        add(UiUtil.scroll(table), BorderLayout.CENTER);
        add(UiUtil.buttonRow(refreshBtn), BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        loadMemberFilter();
        Member member = selectedMember();
        try {
            List<SavingsReport> report = SavingsDAO.report(
                    member == null ? null : member.getMemberId());
            Object[][] rows = new Object[report.size()][];
            double grandTotal = 0;
            int grandMonths = 0;
            for (int i = 0; i < report.size(); i++) {
                SavingsReport r = report.get(i);
                grandTotal += r.getTotal().doubleValue();
                grandMonths += r.getMonths();
                rows[i] = new Object[]{
                        r.getMemberName(),
                        r.getMonths(),
                        String.format("%,.0f", r.getTotal().doubleValue()),
                        String.format("%,.2f", r.getAvgPerMonth().doubleValue()),
                        r.getFirstMonth(),
                        r.getLastMonth()
                };
            }
            UiUtil.refreshTable(table, rows);
            totalLabel.setText(String.format(
                    "TOTAL SAVINGS: UGX %, .0f   (%d member(s), %d contributions)",
                    grandTotal, report.size(), grandMonths));
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
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

    private Member selectedMember() {
        Object sel = memberFilter.getSelectedItem();
        return sel instanceof Member ? (Member) sel : null;
    }

    private Long selectedMemberId() {
        Member m = selectedMember();
        return m == null ? null : m.getMemberId();
    }
}
