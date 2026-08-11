package saccolink.gui.panel;

import saccolink.gui.Refreshable;
import saccolink.model.VerificationResult;
import saccolink.service.PassportService;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

/**
 * Page 10 - Bank Verification.
 * Enter the QR token in the textbox and click Verify; FN_VERIFY_PASSPORT is
 * called and the result is shown.
 */
public class BankVerificationPanel extends JPanel implements Refreshable {

    private final JTextField tokenField = new JTextField(40);
    private final JButton verify = new JButton("Verify");
    private final JLabel status = new JLabel("-");
    private final JLabel member = new JLabel("-");
    private final JLabel score = new JLabel("-");
    private final JLabel band = new JLabel("-");
    private final JLabel generated = new JLabel("-");
    private final JLabel expires = new JLabel("-");

    public BankVerificationPanel() {
        super(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = UiUtil.grid();
        UiUtil.addField(form, g, 0, "QR token", tokenField);
        g.gridx = 3; g.gridy = 0; g.gridwidth = 1; g.weightx = 0;
        form.add(verify, g);
        g.weightx = 1;

        JPanel result = UiUtil.titled("Verification result");
        result.setLayout(new GridBagLayout());
        GridBagConstraints rg = UiUtil.grid();
        int y = 0;
        UiUtil.addField(result, rg, y++, "Status", status);
        UiUtil.addField(result, rg, y++, "Member", member);
        UiUtil.addField(result, rg, y++, "Score", score);
        UiUtil.addField(result, rg, y++, "Band", band);
        UiUtil.addField(result, rg, y++, "Generated at", generated);
        UiUtil.addField(result, rg, y++, "Expires at", expires);

        status.setFont(status.getFont().deriveFont(java.awt.Font.BOLD, 16f));

        verify.addActionListener(this::verify);
        tokenField.addActionListener(this::verify);

        JPanel hint = new JPanel(new BorderLayout());
        hint.add(UiUtil.hint(
                "Enter or paste a QR token and click Verify. "
                + "A VALID result increments the passport's view count in the log."),
                BorderLayout.CENTER);

        add(form, BorderLayout.NORTH);
        add(result, BorderLayout.CENTER);
        add(hint, BorderLayout.SOUTH);
    }

    private void verify(ActionEvent e) {
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            UiUtil.showError(this, "Missing token", "Enter or paste a QR token first.");
            return;
        }
        verify.setEnabled(false);
        UiUtil.runAsync(this, "Verification failed", () -> {
            try {
                VerificationResult r = PassportService.verify(token);
                java.awt.EventQueue.invokeLater(() -> {
                    status.setText(r.getStatus());
                    status.setForeground(r.isValid()
                            ? new java.awt.Color(0x1E7E34)
                            : new java.awt.Color(0xB03030));
                    member.setText(r.getMemberName() == null ? "-" : r.getMemberName());
                    score.setText(r.getScoreValue() == null ? "-"
                            : String.format("%,.2f / 850", r.getScoreValue()));
                    band.setText(r.getScoreBand() == null ? "-" : r.getScoreBand());
                    generated.setText(r.getGeneratedAt() == null ? "-" : format(r.getGeneratedAt()));
                    expires.setText(r.getExpiresAt() == null ? "-" : format(r.getExpiresAt()));
                    verify.setEnabled(true);
                });
            } catch (SQLException ex) {
                java.awt.EventQueue.invokeLater(() -> {
                    verify.setEnabled(true);
                    UiUtil.showError(this, "Verification failed", UiUtil.sqlMessage(ex));
                });
            }
        });
    }

    private static String format(java.sql.Timestamp t) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(t);
    }

    @Override
    public void refresh() {
        // stateless page - nothing to reload
    }
}
