package saccolink.gui.panel;

import saccolink.dao.ScoreDAO;
import saccolink.gui.Refreshable;
import saccolink.model.CreditScore;
import saccolink.util.UiUtil;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Page 8 - Score Display (interactive report over CREDIT_SCORE).
 * Shows current scores (IS_CURRENT = 'Y') with sub-scores and bands.
 * The search box + Search button filter the displayed rows.
 */
public class ScoreDisplayPanel extends JPanel implements Refreshable {

    private static final String[] COLS =
            {"Member", "Score", "Band", "Repayment (60%)", "Savings (40%)", "Computed"};

    private final JTable table = UiUtil.table(COLS);
    private final JTextField search = new JTextField(20);
    private final JButton searchBtn = new JButton("Search");
    private final DecimalFormat fmt = new DecimalFormat("#,##0.00");

    public ScoreDisplayPanel() {
        super(new BorderLayout(8, 8));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Search (member/band/score):"));
        filters.add(search);
        filters.add(searchBtn);

        searchBtn.addActionListener(e -> refresh());
        search.addActionListener(e -> refresh());

        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);

        add(filters, BorderLayout.NORTH);
        add(UiUtil.scroll(table), BorderLayout.CENTER);
        add(UiUtil.buttonRow(refreshBtn), BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        try {
            List<CreditScore> all = ScoreDAO.findCurrent();
            List<CreditScore> scores = all;
            if (!q.isEmpty()) {
                scores = new ArrayList<>();
                for (CreditScore s : all) {
                    if (scoreMatches(s, q)) {
                        scores.add(s);
                    }
                }
            }
            Object[][] rows = new Object[scores.size()][];
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (int i = 0; i < scores.size(); i++) {
                CreditScore s = scores.get(i);
                rows[i] = new Object[]{
                        s.getMemberName(),
                        fmt.format(s.getScoreValue()),
                        s.getScoreBand(),
                        s.getRepaymentScore() == null ? "-" : fmt.format(s.getRepaymentScore()),
                        s.getSavingsScore() == null ? "-" : fmt.format(s.getSavingsScore()),
                        s.getComputedAt() == null ? "" : sdf.format(s.getComputedAt())
                };
            }
            UiUtil.refreshTable(table, rows);
        } catch (SQLException e) {
            UiUtil.showError(this, "Database error", UiUtil.sqlMessage(e));
        }
    }

    private boolean scoreMatches(CreditScore s, String q) {
        return (s.getMemberName() != null && s.getMemberName().toLowerCase().contains(q))
                || (s.getScoreBand() != null && s.getScoreBand().toLowerCase().contains(q))
                || String.valueOf(s.getScoreValue()).contains(q)
                || fmt.format(s.getScoreValue()).contains(q);
    }
}
