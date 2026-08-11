package saccolink.gui.panel;

import saccolink.dao.StatsDAO;
import saccolink.gui.Refreshable;
import saccolink.model.DashboardStats;
import saccolink.session.Session;
import saccolink.util.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;

/**
 * Dashboard - organisation totals (SACCO) or the member's own summary.
 * Registered as the first page for both roles.
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private final JLabel greeting = new JLabel(" ");
    private final JPanel cards = new JPanel(new GridLayout(0, 3, 12, 12));

    public DashboardPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        greeting.setFont(greeting.getFont().deriveFont(Font.BOLD, 18f));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JPanel top = new JPanel(new BorderLayout());
        top.add(greeting, BorderLayout.WEST);
        top.add(refreshBtn, BorderLayout.EAST);
        top.add(Box.createVerticalStrut(6), BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        cards.removeAll();
        try {
            if (Session.isSacco()) {
                DashboardStats s = StatsDAO.saccoTotals();
                greeting.setText("Organisation overview");
                addCard("Registered members", String.valueOf(s.getMemberCount()));
                addCard("Loans recorded", String.valueOf(s.getLoanCount()));
                addCard("Outstanding balance", String.format("%,.0f UGX", s.getTotalOutstanding()));
                addCard("Total savings", String.format("%,.0f UGX", s.getTotalSavings()));
                addCard("Members with a score", String.valueOf(s.getScoredMembers()));
                addCard("Average credit score", String.format("%,.0f", s.getAvgScore()));
                addCard("Pending loan requests", String.valueOf(s.getPendingRequests()),
                        s.getPendingRequests() > 0);
            } else {
                DashboardStats s = StatsDAO.memberTotals();
                String name = Session.user() == null ? "" : Session.user().getDisplayName();
                greeting.setText("Welcome" + (name.isEmpty() ? "" : ", " + name));
                addCard("My loans", String.valueOf(s.getLoanCount()));
                addCard("My outstanding", String.format("%,.0f UGX", s.getTotalOutstanding()));
                addCard("My total savings", String.format("%,.0f UGX", s.getTotalSavings()));
                addCard("My passports", String.valueOf(s.getPassportCount()));
                addCard("My credit score",
                        s.getScoreValue() == null ? "--" : String.format("%,.0f", s.getScoreValue()));
                addCard("My score band",
                        s.getScoreBand() == null ? "--" : s.getScoreBand());
            }
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
        cards.revalidate();
        cards.repaint();
    }

    private void addCard(String caption, String value) {
        addCard(caption, value, false);
    }

    private void addCard(String caption, String value, boolean highlight) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(highlight ? new Color(0xC62828) : new Color(0xC5CAE9), highlight ? 2 : 1),
                BorderFactory.createEmptyBorder(16, 14, 16, 14)));
        JLabel cap = new JLabel(caption);
        cap.setForeground(new Color(0x555555));
        JLabel val = new JLabel(value);
        val.setFont(val.getFont().deriveFont(Font.BOLD, 22f));
        val.setForeground(highlight ? new Color(0xC62828) : new Color(0x1F2A44));
        card.add(cap, BorderLayout.NORTH);
        card.add(val, BorderLayout.CENTER);
        cards.add(card);
    }
}
