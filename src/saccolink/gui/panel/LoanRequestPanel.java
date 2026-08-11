package saccolink.gui.panel;

import saccolink.dao.LoanRequestDAO;
import saccolink.gui.Refreshable;
import saccolink.model.LoanRequest;
import saccolink.model.MemberSummary;
import saccolink.model.CreditScore;
import saccolink.service.ScoreService;
import saccolink.session.Session;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Loan Request workflow page.
 *
 * <p>MEMBER: submits a loan request (amount + purpose) and watches its status.
 * SACCO:  reviews every request - checks the member's creditworthiness
 * (recomputes the score and shows loans/savings totals), then APPROVES
 * (which records the loan on LOAN_RECORD as ACTIVE) or REJECTS it.</p>
 */
public class LoanRequestPanel extends JPanel implements Refreshable {

    private static final String[] COLS =
            {"Request ID", "Member", "Amount (UGX)", "Purpose", "Requested", "Status", "Reviewed By"};

    private final JTable table = UiUtil.table(COLS);

    // member mode
    private final JPanel memberForm = new JPanel(new GridBagLayout());
    private final JTextField amount = new JTextField(14);
    private final JTextField purpose = new JTextField(28);
    private final JButton submit = new JButton("Submit Request");

    // sacco mode
    private final JPanel saccoBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JComboBox<String> statusFilter =
            new JComboBox<>(new String[]{UiUtil.ALL, "PENDING", "APPROVED", "REJECTED"});

    private final JButton creditCheck = new JButton("Check Creditworthiness");
    private final JButton approve = new JButton("Approve");
    private final JButton reject = new JButton("Reject");

    private final Runnable onDataChanged;

    public LoanRequestPanel(Runnable onDataChanged) {
        super(new BorderLayout(8, 8));
        this.onDataChanged = onDataChanged;

        GridBagConstraints g = UiUtil.grid();
        UiUtil.addField(memberForm, g, 0, "Amount (UGX)", amount);
        UiUtil.addField(memberForm, g, 1, "Purpose", purpose);
        g.gridx = 3; g.gridy = 0; g.gridwidth = 1; g.weightx = 0;
        memberForm.add(submit, g);
        g.weightx = 1;

        saccoBar.add(new JLabel("Status:"));
        saccoBar.add(statusFilter);

        submit.addActionListener(this::submitRequest);
        statusFilter.addActionListener(e -> refresh());
        creditCheck.addActionListener(this::creditCheck);
        approve.addActionListener(e -> review("APPROVED"));
        reject.addActionListener(e -> review("REJECTED"));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.add(memberForm);
        topArea.add(saccoBar);

        add(topArea, BorderLayout.NORTH);
        add(UiUtil.scroll(table), BorderLayout.CENTER);
        add(UiUtil.buttonRow(creditCheck, approve, reject, refreshBtn), BorderLayout.SOUTH);
        applyRole();
    }

    private void applyRole() {
        boolean member = Session.isMember();
        memberForm.setVisible(member);
        saccoBar.setVisible(!member);
        creditCheck.setVisible(!member);
        approve.setVisible(!member);
        reject.setVisible(!member);
    }

    private void submitRequest(ActionEvent e) {
        Long memberId = Session.memberId();
        if (memberId == null) {
            UiUtil.showError(this, "Not a member", "Only members can request loans.");
            return;
        }
        try {
            double amt = Double.parseDouble(amount.getText().trim());
            if (amt <= 0) {
                throw new NumberFormatException("amount must be positive");
            }
            LoanRequestDAO.submit(memberId, amt, purpose.getText().trim());
            UiUtil.showInfo(this, "Request submitted",
                    "Your loan request for " + String.format("%,.0f", amt)
                    + " UGX has been sent to the SACCO for review.");
            amount.setText("");
            purpose.setText("");
            refresh();
        } catch (NumberFormatException ex) {
            UiUtil.showError(this, "Invalid input", "Amount must be a positive number.");
        } catch (SQLException ex) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(ex));
        }
    }

    private void creditCheck(ActionEvent e) {
        LoanRequest req = selected();
        if (req == null) {
            UiUtil.showInfo(this, "Nothing selected", "Select a loan request row first.");
            return;
        }
        creditCheck.setEnabled(false);
        UiUtil.runAsync(this, "Credit check failed", () -> {
            try {
                // recompute the score so the decision uses fresh data
                ScoreService.compute(req.getMemberId());
                MemberSummary s = LoanRequestDAO.creditCheck(req.getMemberId());
                s.setMemberName(req.getMemberName());
                java.awt.EventQueue.invokeLater(() -> {
                    creditCheck.setEnabled(true);
                    UiUtil.showInfo(this, "Creditworthiness - " + s.getMemberName(),
                            "Member     : " + s.getMemberName() + "\n"
                            + "Loan count : " + s.getLoanCount() + "\n"
                            + "Savings    : " + String.format("%,.0f", s.getTotalSavings()) + " UGX\n"
                            + "Score      : " + (s.hasScore()
                                    ? String.format("%,.2f / 850", s.getScore())
                                    : "not computed yet") + "\n"
                            + "Band       : " + (s.hasScore() ? s.getBand() : "-") + "\n\n"
                            + "Approve to record the loan as ACTIVE, or reject the request.");
                });
            } catch (SQLException ex) {
                java.awt.EventQueue.invokeLater(() -> {
                    creditCheck.setEnabled(true);
                    UiUtil.showError(this, "Credit check failed", UiUtil.sqlMessage(ex));
                });
            }
        });
    }

    private void review(String decision) {
        LoanRequest req = selected();
        if (req == null) {
            UiUtil.showInfo(this, "Nothing selected", "Select a loan request row first.");
            return;
        }
        if (!"PENDING".equals(req.getStatus())) {
            UiUtil.showInfo(this, "Already reviewed",
                    "This request is already " + req.getStatus() + ".");
            return;
        }
        int choice = javax.swing.JOptionPane.showConfirmDialog(this,
                decision.equals("APPROVED")
                        ? "Approve request #" + req.getRequestId() + " for "
                            + req.getMemberName() + "?\nA loan of "
                            + String.format("%,.0f", req.getRequestedAmount())
                            + " UGX will be recorded as ACTIVE."
                        : "Reject request #" + req.getRequestId() + " for "
                            + req.getMemberName() + "?",
                decision.equals("APPROVED") ? "Approve loan" : "Reject loan",
                javax.swing.JOptionPane.YES_NO_OPTION);
        if (choice != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        try {
            LoanRequestDAO.review(req.getRequestId(), decision, Session.username());
            UiUtil.showInfo(this, "Request " + decision,
                    decision.equals("APPROVED")
                            ? "Loan recorded as ACTIVE and the request is now APPROVED."
                            : "The request has been REJECTED.");
            refresh();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (SQLException ex) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(ex));
        }
    }

    private LoanRequest selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        long id = (long) table.getModel().getValueAt(modelRow, 0);
        String status = (String) statusFilter.getSelectedItem();
        try {
            for (LoanRequest r : LoanRequestDAO.findAll(
                    UiUtil.ALL.equals(status) ? null : status)) {
                if (r.getRequestId() == id) {
                    return r;
                }
            }
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
        return null;
    }

    @Override
    public void refresh() {
        applyRole();
        String status = (String) statusFilter.getSelectedItem();
        try {
            List<LoanRequest> reqs = LoanRequestDAO.findAll(
                    UiUtil.ALL.equals(status) ? null : status);
            Object[][] rows = new Object[reqs.size()][];
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (int i = 0; i < reqs.size(); i++) {
                LoanRequest r = reqs.get(i);
                rows[i] = new Object[]{
                        r.getRequestId(),
                        r.getMemberName(),
                        String.format("%,.0f", r.getRequestedAmount()),
                        r.getPurpose() == null ? "" : r.getPurpose(),
                        r.getRequestedAt() == null ? "" : sdf.format(r.getRequestedAt()),
                        r.getStatus(),
                        r.getReviewedBy() == null ? "" : r.getReviewedBy()
                };
            }
            UiUtil.refreshTable(table, rows);
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }
}
