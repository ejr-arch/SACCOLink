package saccolink.gui.panel;

import saccolink.dao.MemberDAO;
import saccolink.dao.SavingsDAO;
import saccolink.model.Member;
import saccolink.model.SavingsRecord;
import saccolink.util.DateUtil;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

/**
 * Page 5 - Savings Entry (form on SAVINGS_RECORD).
 * Contribution month is entered as yyyy-MM and normalised to day 01.
 */
public class SavingsEntryPanel extends JPanel {

    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JTextField month = new JTextField(10);
    private final JTextField amount = new JTextField(14);
    private final JButton save = new JButton("Save");
    private final JButton clear = new JButton("Clear");

    public SavingsEntryPanel() {
        super(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = UiUtil.grid();

        int y = 0;
        UiUtil.addField(form, g, y++, "Member", memberCombo);
        UiUtil.addField(form, g, y++, "Contribution month (yyyy-MM)", month);
        UiUtil.addField(form, g, y++, "Amount contributed (UGX)", amount);

        save.addActionListener(this::save);
        clear.addActionListener(e -> {
            month.setText("");
            amount.setText("");
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(UiUtil.hint(
                "Only one contribution per member per month is allowed (DB enforces it). "
                + "Savings history drives the 40% consistency factor."),
                BorderLayout.CENTER);
        bottom.add(UiUtil.buttonRow(save, clear), BorderLayout.SOUTH);

        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    public void loadMembers() {
        try {
            List<Member> members = MemberDAO.findAll();
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

    private void save(ActionEvent e) {
        Member member = (Member) memberCombo.getSelectedItem();
        if (member == null) {
            UiUtil.showError(this, "No member", "Register a member first (Page 1).");
            return;
        }
        try {
            java.time.YearMonth ym = DateUtil.parseMonth(month.getText());
            double amt = Double.parseDouble(amount.getText().trim());
            if (amt <= 0) {
                throw new NumberFormatException("amount must be positive");
            }

            SavingsRecord s = new SavingsRecord();
            s.setMemberId(member.getMemberId());
            s.setContributionMonth(Date.valueOf(ym.atDay(1)));
            s.setAmountContributed(amt);

            SavingsDAO.insert(s);
            UiUtil.showInfo(this, "Savings saved",
                    "Recorded " + String.format("%,.0f", amt) + " for " + ym + ".");
            month.setText("");
            amount.setText("");
        } catch (DateUtil.InvalidDateException ex) {
            UiUtil.showError(this, "Invalid month", ex.getMessage());
        } catch (NumberFormatException ex) {
            UiUtil.showError(this, "Invalid input", "Amount must be a positive number.");
        } catch (SQLException ex) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(ex));
        }
    }
}
