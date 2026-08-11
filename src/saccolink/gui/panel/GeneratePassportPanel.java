package saccolink.gui.panel;

import saccolink.dao.MemberDAO;
import saccolink.gui.Refreshable;
import saccolink.model.Member;
import saccolink.service.PassportService;
import saccolink.session.Session;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Page 8 - Generate Passport.
 * Calls SP_GENERATE_PASSPORT and displays the returned 32-char QR token
 * (72-hour expiry, consent + current score required).
 */
public class GeneratePassportPanel extends JPanel implements Refreshable {

    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JButton generate = new JButton("Generate New Passport");
    private final JLabel passportId = new JLabel("-");
    private final JLabel token = new JLabel("-");
    private final JLabel expires = new JLabel("-");
    private final JButton copy = new JButton("Copy Token");

    public GeneratePassportPanel() {
        super(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = UiUtil.grid();
        UiUtil.addField(form, g, 0, "Member", memberCombo);
        g.gridx = 3; g.gridy = 0; g.gridwidth = 1; g.weightx = 0;
        UiUtil.primary(generate);
        generate.setToolTipText("Creates a new 72-hour credit passport for the selected member");
        form.add(generate, g);
        g.weightx = 1;

        g.gridx = 0; g.gridy = 1; g.gridwidth = 3;
        form.add(new JLabel("A 72-hour credit passport will be generated for the selected member."), g);

        JPanel result = UiUtil.titled("Generated passport");
        result.setLayout(new GridBagLayout());
        GridBagConstraints rg = UiUtil.grid();
        int y = 0;
        UiUtil.addField(result, rg, y++, "Passport ID", passportId);
        UiUtil.addField(result, rg, y++, "QR token", token);
        UiUtil.addField(result, rg, y++, "Expires at", expires);

        token.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 16));
        token.setForeground(new java.awt.Color(0x0B5CAD));

        copy.setEnabled(false);
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(token.getText()), null);
        });

        generate.addActionListener(this::generate);

        add(form, BorderLayout.NORTH);
        add(result, BorderLayout.CENTER);
        add(UiUtil.buttonRow(copy), BorderLayout.SOUTH);
    }

    private void generate(ActionEvent e) {
        Member member = (Member) memberCombo.getSelectedItem();
        if (member == null) {
            UiUtil.showError(this, "No member", "Register a member first (Page 1).");
            return;
        }
        if (!member.isConsentGiven()) {
            UiUtil.showError(this, "Consent required",
                    member.getFullName() + " has NOT given consent.\n"
                    + "Update consent on the Member Registration page first.");
            return;
        }
        generate.setEnabled(false);
        UiUtil.runAsync(this, "Generation failed", () -> {
            try {
                Object[] out = PassportService.generate(member.getMemberId());
                long id = (long) out[0];
                String tok = (String) out[1];
                java.awt.EventQueue.invokeLater(() -> {
                    passportId.setText(String.valueOf(id));
                    token.setText(tok);
                    expires.setText("72 hours from now");
                    copy.setEnabled(true);
                    generate.setEnabled(true);
                    UiUtil.showInfo(this, "Passport generated",
                            "Credit passport #" + id + " created.\n"
                            + "Share the QR token with the verifying bank.\n"
                            + "It expires automatically after 72 hours.");
                });
            } catch (SQLException ex) {
                java.awt.EventQueue.invokeLater(() -> {
                    generate.setEnabled(true);
                    UiUtil.showError(this, "Generation failed", UiUtil.sqlMessage(ex));
                });
            }
        });
    }

    @Override
    public void refresh() {
        Member selected = (Member) memberCombo.getSelectedItem();
        Long keep = selected == null ? null : selected.getMemberId();
        try {
            List<Member> members;
            if (Session.isMember()) {
                members = new java.util.ArrayList<>();
                Member self = MemberDAO.findById(Session.memberId());
                if (self != null) {
                    members.add(self);
                }
            } else {
                members = MemberDAO.findAll();
            }
            UiUtil.populateCombo(memberCombo, members);
            for (Member m : members) {
                if (m.getMemberId().equals(keep)) {
                    memberCombo.setSelectedItem(m);
                    break;
                }
            }
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }
}
