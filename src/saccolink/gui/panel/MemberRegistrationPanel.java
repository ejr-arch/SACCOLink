package saccolink.gui.panel;

import saccolink.dao.MemberDAO;
import saccolink.model.Member;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

/**
 * Page 1 - Member Registration (create / edit on MEMBER).
 * Mirrors the APEX "Form on MEMBER" page.
 */
public class MemberRegistrationPanel extends JPanel {

    private final JTextField nin = new JTextField(16);
    private final JTextField fullName = new JTextField(24);
    private final JTextField phone = new JTextField(16);
    private final JTextField district = new JTextField(20);
    private final JCheckBox consent = new JCheckBox("I consent to sharing my credit data");
    private final JLabel modeLabel = new JLabel("New member");
    private final JButton save = new JButton("Save");
    private final JButton clear = new JButton("Clear");

    private Long editingId;

    public MemberRegistrationPanel() {
        super(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = UiUtil.grid();

        g.gridx = 0; g.gridy = 0; g.gridwidth = 3; g.weightx = 0;
        modeLabel.setFont(modeLabel.getFont().deriveFont(java.awt.Font.BOLD));
        form.add(modeLabel, g);
        g.weightx = 1;

        int y = 1;
        UiUtil.addField(form, g, y++, "NIN (14 chars)", nin);
        UiUtil.addField(form, g, y++, "Full name", fullName);
        UiUtil.addField(form, g, y++, "Phone number", phone);
        UiUtil.addField(form, g, y++, "District", district);
        g.gridx = 1; g.gridy = y; g.gridwidth = 2;
        form.add(consent, g);

        save.addActionListener(this::save);
        clear.addActionListener(e -> resetForm());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(UiUtil.hint(
                "NIN follows NIRA format: 14 chars, e.g. CM012345678901. "
                + "Consent is required before a credit passport can be generated."),
                BorderLayout.CENTER);
        bottom.add(UiUtil.buttonRow(save, clear), BorderLayout.SOUTH);

        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    /** Loads a member into edit mode. */
    public void loadMember(Member m) {
        editingId = m.getMemberId();
        nin.setText(m.getNin());
        fullName.setText(m.getFullName());
        phone.setText(m.getPhoneNumber());
        district.setText(m.getDistrict());
        consent.setSelected(m.isConsentGiven());
        modeLabel.setText("Editing member #" + editingId + "  (save to update)");
    }

    public void resetForm() {
        editingId = null;
        nin.setText("");
        fullName.setText("");
        phone.setText("");
        district.setText("");
        consent.setSelected(false);
        modeLabel.setText("New member");
    }

    private void save(ActionEvent e) {
        Member m = new Member();
        m.setMemberId(editingId);
        m.setNin(nin.getText());
        m.setFullName(fullName.getText());
        m.setPhoneNumber(phone.getText());
        m.setDistrict(district.getText());
        m.setConsentGiven(consent.isSelected());

        try {
            if (editingId == null) {
                long id = MemberDAO.insert(m);
                UiUtil.showInfo(this, "Member saved",
                        "Member registered with ID " + id + ".");
            } else {
                MemberDAO.update(m);
                UiUtil.showInfo(this, "Member updated", "Changes saved.");
            }
            resetForm();
        } catch (IllegalArgumentException ex) {
            UiUtil.showError(this, "Validation error", ex.getMessage());
        } catch (SQLException ex) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(ex));
        }
    }
}
