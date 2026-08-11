package saccolink.gui.panel;

import saccolink.dao.MemberDAO;
import saccolink.dao.ScoreDAO;
import saccolink.gui.Refreshable;
import saccolink.model.CreditScore;
import saccolink.model.Member;
import saccolink.service.ScoreService;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Page 7 - Compute Score.
 * Search box + Search button filter the member list; the Compute Score button
 * calls SP_COMPUTE_SCORE and shows the resulting score + band.
 */
public class ComputeScorePanel extends JPanel implements Refreshable {

    private final JTextField searchField = new JTextField(24);
    private final JButton searchBtn = new JButton("Search");
    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JTextArea result = new JTextArea(6, 50);
    private final JButton compute = new JButton("Compute Score");

    public ComputeScorePanel() {
        super(new BorderLayout(8, 8));

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints g = UiUtil.grid();

        int y = 0;
        // search row
        g.gridx = 0; g.gridy = y; g.gridwidth = 1;
        top.add(new JLabel("Search member (name/NIN):"), g);
        g.gridx = 1;
        top.add(searchField, g);
        g.gridx = 2; g.gridwidth = 1; g.weightx = 0;
        top.add(searchBtn, g);
        g.weightx = 1;

        // member row
        y++;
        g.gridx = 0; g.gridy = y;
        top.add(new JLabel("Member:"), g);
        g.gridx = 1;
        top.add(memberCombo, g);
        g.gridx = 2; g.weightx = 0;
        top.add(compute, g);
        g.weightx = 1;

        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setBorder(javax.swing.BorderFactory.createTitledBorder("Result"));

        searchBtn.addActionListener(e -> filterMembers());
        searchField.addActionListener(e -> filterMembers());
        compute.addActionListener(this::compute);

        JPanel hint = new JPanel(new BorderLayout());
        hint.add(UiUtil.hint(
                "Formula (as in SP_COMPUTE_SCORE):\n"
                + "  Repayment (60%)  = repaid/total loans x100, minus 25 per default\n"
                + "  Savings (40%)    = months contributed / months since first x100\n"
                + "  Composite        = (0.60 x repayment + 0.40 x savings) x 8.5  ->  0-850\n"
                + "  Bands: EXCELLENT >=700 | GOOD 550-699 | FAIR 400-549 | THIN <400"),
                BorderLayout.NORTH);
        hint.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 8, 8));

        add(top, BorderLayout.NORTH);
        add(result, BorderLayout.CENTER);
        add(hint, BorderLayout.SOUTH);
    }

    /** Reloads members and keeps only those matching the search box. */
    private void filterMembers() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        Member selected = (Member) memberCombo.getSelectedItem();
        Long keep = selected == null ? null : selected.getMemberId();
        try {
            List<Member> filtered = new ArrayList<>();
            for (Member m : MemberDAO.findAll()) {
                if (q.isEmpty() || matches(m, q)) {
                    filtered.add(m);
                }
            }
            UiUtil.populateCombo(memberCombo, filtered);
            if (keep != null) {
                for (Member m : filtered) {
                    if (m.getMemberId().equals(keep)) {
                        memberCombo.setSelectedItem(m);
                        break;
                    }
                }
            } else if (!filtered.isEmpty()) {
                memberCombo.setSelectedIndex(0);
            }
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }

    private static boolean matches(Member m, String q) {
        return (m.getFullName() != null && m.getFullName().toLowerCase().contains(q))
                || (m.getNin() != null && m.getNin().toLowerCase().contains(q))
                || String.valueOf(m.getMemberId()).equals(q);
    }

    private void compute(ActionEvent e) {
        Member member = (Member) memberCombo.getSelectedItem();
        if (member == null) {
            UiUtil.showError(this, "No member",
                    "No member found. Clear the search box or register a member first (Page 1).");
            return;
        }
        compute.setEnabled(false);
        result.setText("Computing...");
        UiUtil.runAsync(this, "Compute failed", () -> {
            try {
                CreditScore score = ScoreService.compute(member.getMemberId());
                java.awt.EventQueue.invokeLater(() -> {
                    result.setText("Score for " + member.getFullName() + ":\n"
                            + "  Score value : " + score.getScoreValue() + " / 850\n"
                            + "  Band        : " + score.getScoreBand() + "\n"
                            + "  Repayment   : " + score.getRepaymentScore() + " / 100\n"
                            + "  Savings     : " + score.getSavingsScore() + " / 100\n"
                            + "  Computed at : " + score.getComputedAt()
                            + "\n\nView all current scores on the Score Display page.");
                    compute.setEnabled(true);
                });
            } catch (SQLException ex) {
                java.awt.EventQueue.invokeLater(() -> {
                    compute.setEnabled(true);
                    result.setText("");
                    UiUtil.showError(this, "Compute failed", UiUtil.sqlMessage(ex));
                });
            }
        });
    }

    @Override
    public void refresh() {
        filterMembers();
    }
}
