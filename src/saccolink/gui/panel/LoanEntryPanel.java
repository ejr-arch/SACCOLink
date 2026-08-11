package saccolink.gui.panel;

import saccolink.dao.LoanDAO;
import saccolink.dao.MemberDAO;
import saccolink.model.LoanRecord;
import saccolink.model.Member;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Page 3 - Loan Entry (form on LOAN_RECORD).
 * Member is chosen from a select list of MEMBER rows.
 */
public class LoanEntryPanel extends JPanel {

    private static final String[] STATUSES = {"ACTIVE", "REPAID", "DEFAULTED"};

    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JTextField amount = new JTextField(14);
    private final JTextField disbursement = new JTextField(12);
    private final JTextField repayment = new JTextField(12);
    private final JComboBox<String> status = new JComboBox<>(STATUSES);
    private final JLabel modeLabel = new JLabel("New loan");
    private final JButton save = new JButton("Save");
    private final JButton clear = new JButton("Clear");

    private Long editingLoanId;

    public LoanEntryPanel() {
        super(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = UiUtil.grid();

        g.gridx = 0; g.gridy = 0; g.gridwidth = 3; g.weightx = 0;
        modeLabel.setFont(modeLabel.getFont().deriveFont(java.awt.Font.BOLD));
        form.add(modeLabel, g);
        g.weightx = 1;

        int y = 1;
        UiUtil.addField(form, g, y++, "Member", memberCombo);
        UiUtil.addField(form, g, y++, "Loan amount (UGX)", amount);
        UiUtil.addField(form, g, y++, "Disbursement date (yyyy-MM-dd)", disbursement);
        UiUtil.addField(form, g, y++, "Repayment date (yyyy-MM-dd)", repayment);
        UiUtil.addField(form, g, y++, "Loan status", status);

        save.addActionListener(this::save);
        clear.addActionListener(e -> resetForm());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(UiUtil.hint(
                "Leave repayment date blank while the loan is ACTIVE. "
                + "Loan status drives the 60% repayment factor in the score."),
                BorderLayout.CENTER);
        bottom.add(UiUtil.buttonRow(save, clear), BorderLayout.SOUTH);

        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    /** Loads members into the combo (called before the page is shown). */
    public void loadMembers() {
        try {
            List<Member> members = MemberDAO.findAll();
            if (members.isEmpty()) {
                memberCombo.removeAllItems();
                return;
            }
            Member selected = (Member) memberCombo.getSelectedItem();
            UiUtil.populateCombo(memberCombo, members);
            if (selected != null) {
                for (Member m : members) {
                    if (m.getMemberId().equals(selected.getMemberId())) {
                        memberCombo.setSelectedItem(m);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }

    public void loadLoan(LoanRecord loan) {
        editingLoanId = loan.getLoanId();
        loadMembers();
        Member target = null;
        for (int i = 0; i < memberCombo.getItemCount(); i++) {
            Member m = memberCombo.getItemAt(i);
            if (m.getMemberId().equals(loan.getMemberId())) {
                target = m;
                break;
            }
        }
        if (target != null) {
            memberCombo.setSelectedItem(target);
        }
        amount.setText(String.format("%.0f", loan.getLoanAmount()));
        disbursement.setText(loan.getDisbursementDate() == null ? "" : loan.getDisbursementDate().toString());
        repayment.setText(loan.getRepaymentDate() == null ? "" : loan.getRepaymentDate().toString());
        status.setSelectedItem(loan.getLoanStatus());
        modeLabel.setText("Editing loan #" + editingLoanId + "  (save to update)");
    }

    public void resetForm() {
        editingLoanId = null;
        loadMembers();
        amount.setText("");
        disbursement.setText("");
        repayment.setText("");
        status.setSelectedItem("ACTIVE");
        modeLabel.setText("New loan");
    }

    private void save(ActionEvent e) {
        Member member = (Member) memberCombo.getSelectedItem();
        if (member == null) {
            UiUtil.showError(this, "No member", "Register a member first (Page 1).");
            return;
        }
        try {
            double amt = Double.parseDouble(amount.getText().trim());
            if (amt <= 0) {
                throw new NumberFormatException("amount must be positive");
            }
            Date disb = parseDate(disbursement.getText(), "disbursement date");
            Date repay = parseDateOrNull(repayment.getText(), "repayment date");

            LoanRecord loan = new LoanRecord();
            loan.setLoanId(editingLoanId);
            loan.setMemberId(member.getMemberId());
            loan.setLoanAmount(amt);
            loan.setDisbursementDate(disb);
            loan.setRepaymentDate(repay);
            loan.setLoanStatus((String) status.getSelectedItem());

            if (editingLoanId == null) {
                long id = LoanDAO.insert(loan);
                UiUtil.showInfo(this, "Loan saved", "Loan recorded with ID " + id + ".");
            } else {
                LoanDAO.update(loan);
                UiUtil.showInfo(this, "Loan updated", "Changes saved.");
            }
            resetForm();
        } catch (NumberFormatException ex) {
            UiUtil.showError(this, "Invalid input", "Amount and dates must be valid: " + ex.getMessage());
        } catch (DateTimeParseException | saccolink.util.DateUtil.InvalidDateException ex) {
            UiUtil.showError(this, "Invalid date", ex.getMessage());
        } catch (SQLException ex) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(ex));
        }
    }

    private static Date parseDate(String text, String what)
            throws saccolink.util.DateUtil.InvalidDateException {
        return Date.valueOf(saccolink.util.DateUtil.parse(text, what));
    }

    private static Date parseDateOrNull(String text, String what)
            throws saccolink.util.DateUtil.InvalidDateException {
        if (text == null || text.isBlank()) {
            return null;
        }
        return parseDate(text, what);
    }
}
