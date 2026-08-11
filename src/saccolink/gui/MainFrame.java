package saccolink.gui;

import saccolink.db.DBConnection;
import saccolink.gui.panel.BankVerificationPanel;
import saccolink.gui.panel.ComputeScorePanel;
import saccolink.gui.panel.DashboardPanel;
import saccolink.gui.panel.GeneratePassportPanel;
import saccolink.gui.panel.LoanEntryPanel;
import saccolink.gui.panel.LoanListPanel;
import saccolink.gui.panel.LoanRequestPanel;
import saccolink.gui.panel.MemberListPanel;
import saccolink.gui.panel.MemberRegistrationPanel;
import saccolink.gui.panel.PassportLogPanel;
import saccolink.gui.panel.SavingsEntryPanel;
import saccolink.gui.panel.SavingsListPanel;
import saccolink.gui.panel.ScoreDisplayPanel;
import saccolink.model.AppUser;
import saccolink.model.LoanRecord;
import saccolink.model.Member;
import saccolink.session.Session;
import saccolink.util.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main application window. A left sidebar mirrors the desktop navigation and
 * adapts to the logged-in role: a MEMBER only sees their own history, while
 * SACCO staff get full access plus the loan-request review workflow.
 */
public class MainFrame extends JFrame {

    /** Page definitions per role: pageId + menu title. */
    private static final String[][] SACCO_PAGES = {
            {"dashboard", "Dashboard"},
            {"member-registration", "Member Registration"},
            {"member-list", "Member List"},
            {"loan-entry", "Loan Entry"},
            {"loan-list", "Loan List"},
            {"savings-entry", "Savings Entry"},
            {"savings-list", "Savings Report"},
            {"compute-score", "Compute Score"},
            {"score-display", "Score Display"},
            {"generate-passport", "Generate Passport"},
            {"bank-verification", "Bank Verification"},
            {"passport-log", "Passport Log"},
            {"loan-requests", "Loan Requests"},
    };

    private static final String[][] MEMBER_PAGES = {
            {"dashboard", "My Dashboard"},
            {"loan-list", "My Loans"},
            {"savings-list", "My Savings Report"},
            {"score-display", "My Score"},
            {"loan-requests", "Request Loan"},
            {"passport-log", "My Passports"},
            {"generate-passport", "Generate Passport"},
    };

    private static final Map<String, String> SUBTITLES = new HashMap<>();

    static {
        SUBTITLES.put("dashboard",
                "Organisation totals for SACCO staff; your personal summary for members.");
        SUBTITLES.put("member-registration", "Create a new member or edit an existing one.");
        SUBTITLES.put("member-list", "Search, filter and manage registered members.");
        SUBTITLES.put("loan-entry", "Record or update a loan on LOAN_RECORD.");
        SUBTITLES.put("loan-list", "All loans with member, status and keyword filters.");
        SUBTITLES.put("savings-entry", "Record a monthly savings contribution.");
        SUBTITLES.put("savings-list", "Aggregate savings report across all members.");
        SUBTITLES.put("compute-score", "Recompute a member's credit score (0 - 850).");
        SUBTITLES.put("score-display", "Current credit scores, sub-scores and bands.");
        SUBTITLES.put("generate-passport", "Issue a 72-hour credit passport QR token.");
        SUBTITLES.put("bank-verification", "Verify a QR token through FN_VERIFY_PASSPORT.");
        SUBTITLES.put("passport-log", "All credit passports, tokens and their statuses.");
        SUBTITLES.put("loan-requests", "Submit a request (MEMBER) or review one (SACCO).");
    }

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel nav = new JPanel();
    private final JLabel statusBar = new JLabel("Not connected");
    private final JLabel pageTitle = new JLabel(" ");
    private final JLabel pageSubtitle = new JLabel(" ");
    private final JLabel roleBadge = new JLabel(" ");

    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private String currentPageId;

    private final MemberRegistrationPanel regPanel = new MemberRegistrationPanel();
    private final MemberListPanel listMembers = new MemberListPanel(this);
    private final LoanEntryPanel loanEntry = new LoanEntryPanel();
    private final LoanListPanel loanList = new LoanListPanel(this);
    private final SavingsEntryPanel savingsEntry = new SavingsEntryPanel();
    private final SavingsListPanel savingsList = new SavingsListPanel();
    private final ComputeScorePanel computeScore = new ComputeScorePanel();
    private final ScoreDisplayPanel scoreDisplay = new ScoreDisplayPanel();
    private final GeneratePassportPanel generatePassport = new GeneratePassportPanel();
    private final BankVerificationPanel verify = new BankVerificationPanel();
    private final PassportLogPanel passportLog = new PassportLogPanel();
    private final LoanRequestPanel loanRequests = new LoanRequestPanel(this::refreshAll);
    private final DashboardPanel dashboard = new DashboardPanel();

    public MainFrame() {
        super("SACCOLink - Credit Passport System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);

        setJMenuBar(buildMenu());

        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setPreferredSize(new Dimension(240, 0));
        nav.setBackground(UiUtil.NAV_BG);
        add(nav, BorderLayout.WEST);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(buildPageHeader(), BorderLayout.NORTH);
        contentWrapper.add(content, BorderLayout.CENTER);
        add(contentWrapper, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);

        register("member-registration", regPanel);
        register("member-list", listMembers);
        register("loan-entry", loanEntry);
        register("loan-list", loanList);
        register("savings-entry", savingsEntry);
        register("savings-list", savingsList);
        register("compute-score", computeScore);
        register("score-display", scoreDisplay);
        register("generate-passport", generatePassport);
        register("bank-verification", verify);
        register("passport-log", passportLog);
        register("loan-requests", loanRequests);
        register("dashboard", dashboard);

        rebuildNav();
        showPage("member-registration");
    }

    /** Registers a page in the card layout (data is loaded after connecting). */
    private void register(String id, JPanel panel) {
        content.add(panel, id);
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem connect = new JMenuItem("Connect...");
        connect.addActionListener(e -> openConnectionDialog());
        JMenuItem reconnect = new JMenuItem("Reconnect");
        reconnect.addActionListener(e -> {
            if (openConnectionDialog()) {
                refreshAll();
            }
        });
        JMenuItem logout = new JMenuItem("Logout / Switch User");
        logout.addActionListener(e -> logout());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        file.add(connect);
        file.add(reconnect);
        file.addSeparator();
        file.add(logout);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "SACCOLink - Credit Passport System\n"
                + "Recess 2026 | Makerere University | EJr Tech Solutions\n"
                + "Oracle DB 18c XE | JDBC | Java Swing\n\n"
                + "Two account types:\n"
                + "  MEMBER - only their own loans / savings / score / passports,\n"
                + "           can request a loan and generate a passport token.\n"
                + "  SACCO  - full access; reviews loan requests and checks\n"
                + "           creditworthiness before approving.",
                "About SACCOLink", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        bar.add(file);
        bar.add(help);
        return bar;
    }

    /** (Re)builds the left navigation for the currently logged-in role. */
    private void rebuildNav() {
        nav.removeAll();
        navButtons.clear();

        addBrand();
        if (Session.isLoggedIn()) {
            nav.add(Box.createVerticalStrut(4));
            nav.add(buildUserChip());
        }
        nav.add(Box.createVerticalStrut(14));

        JLabel section = new JLabel("MENU");
        section.setForeground(new Color(0x9DB8B2));
        section.setFont(section.getFont().deriveFont(Font.BOLD, 11f));
        section.setBorder(BorderFactory.createEmptyBorder(0, 18, 6, 0));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(section);

        String[][] pages = Session.isSacco() ? SACCO_PAGES : MEMBER_PAGES;
        for (int i = 0; i < pages.length; i++) {
            addNav((i + 1) + ".  " + pages[i][1], pages[i][0]);
        }

        nav.add(Box.createVerticalStrut(12));
        nav.add(buildLogoutButton());
        nav.add(Box.createVerticalGlue());

        JLabel credit = new JLabel("Recess 2026 | Makerere University");
        credit.setForeground(new Color(0x8FAFA8));
        credit.setFont(credit.getFont().deriveFont(Font.PLAIN, 11f));
        credit.setAlignmentX(Component.LEFT_ALIGNMENT);
        credit.setBorder(BorderFactory.createEmptyBorder(0, 18, 14, 0));
        nav.add(credit);

        nav.revalidate();
        nav.repaint();

        if (currentPageId != null) {
            showPage(currentPageId);
        }
    }

    private void addBrand() {
        JLabel brand = new JLabel("SACCOLink");
        brand.setForeground(Color.WHITE);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 21f));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setBorder(BorderFactory.createEmptyBorder(18, 18, 2, 8));

        JLabel tagline = new JLabel("Credit Passport System");
        tagline.setForeground(new Color(0x9DB8B2));
        tagline.setFont(tagline.getFont().deriveFont(Font.PLAIN, 12f));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagline.setBorder(BorderFactory.createEmptyBorder(0, 18, 4, 8));

        nav.add(brand);
        nav.add(tagline);
    }

    /** Avatar + name/role chip shown in the sidebar for the logged-in user. */
    private JPanel buildUserChip() {
        JPanel chip = new JPanel(new BorderLayout(10, 0));
        chip.setOpaque(true);
        chip.setBackground(new Color(0x003830));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 14, 10, 10),
                BorderFactory.createLineBorder(new Color(0x0A5C4E))));
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);

        AppUser u = Session.user();
        String name = u == null ? "" : u.getDisplayName();
        JLabel avatar = new JLabel(initials(name), SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(UiUtil.ACCENT);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(avatar.getFont().deriveFont(Font.BOLD, 13f));
        avatar.setPreferredSize(new Dimension(32, 32));
        avatar.setBorder(BorderFactory.createLineBorder(new Color(0x2E8B57)));
        chip.add(avatar, BorderLayout.WEST);

        JLabel nm = new JLabel(name.isEmpty() ? " " : name);
        nm.setForeground(Color.WHITE);
        nm.setFont(nm.getFont().deriveFont(Font.BOLD, 13f));
        JLabel role = new JLabel(Session.isSacco() ? "SACCO Staff" : "Member");
        role.setForeground(new Color(0x9DB8B2));
        role.setFont(role.getFont().deriveFont(Font.PLAIN, 11f));
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(nm);
        names.add(role);
        chip.add(names, BorderLayout.CENTER);
        return chip;
    }

    private static String initials(String name) {
        String[] parts = (name == null ? "" : name.trim()).split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0].charAt(0));
        if (parts.length > 1) {
            sb.append(parts[1].charAt(0));
        }
        return sb.toString().toUpperCase();
    }

    private void addNav(String label, String pageId) {
        JButton b = UiUtil.navItem(label);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!pageId.equals(currentPageId)) {
                    b.setBackground(UiUtil.NAV_HOVER);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                UiUtil.styleNav(b, pageId.equals(currentPageId));
            }
        });
        b.addActionListener(e -> showPage(pageId));
        navButtons.put(pageId, b);
        nav.add(b);
    }

    private JButton buildLogoutButton() {
        JButton logout = UiUtil.navItem("Logout");
        logout.setForeground(new Color(0xFFAB91));
        logout.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 12));
        logout.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                logout.setBackground(new Color(0x005043));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                logout.setBackground(UiUtil.NAV_BG);
            }
        });
        logout.addActionListener(e -> logout());
        return logout;
    }

    /** Shared page header: title + subtitle on the left, role badge on the right. */
    private JPanel buildPageHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiUtil.BORDER),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        pageTitle.setFont(pageTitle.getFont().deriveFont(Font.BOLD, 17f));
        pageTitle.setForeground(UiUtil.PRIMARY);
        pageSubtitle.setFont(pageSubtitle.getFont().deriveFont(Font.PLAIN, 12f));
        pageSubtitle.setForeground(UiUtil.TEXT_MUTED);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(pageTitle);
        titles.add(pageSubtitle);
        header.add(titles, BorderLayout.CENTER);

        roleBadge.setOpaque(true);
        roleBadge.setForeground(Color.WHITE);
        roleBadge.setFont(roleBadge.getFont().deriveFont(Font.BOLD, 11f));
        roleBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2E8B57)),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        header.add(roleBadge, BorderLayout.EAST);
        return header;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiUtil.BG);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiUtil.BORDER));

        statusBar.setFont(statusBar.getFont().deriveFont(Font.PLAIN, 11f));
        statusBar.setForeground(UiUtil.TEXT_MUTED);
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        bar.add(statusBar, BorderLayout.WEST);
        updateStatus();
        return bar;
    }

    public void showPage(String pageId) {
        cards.show(content, pageId);
        currentPageId = pageId;

        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            UiUtil.styleNav(e.getValue(), e.getKey().equals(pageId));
        }
        pageTitle.setText(titleOf(pageId));
        pageSubtitle.setText(SUBTITLES.getOrDefault(pageId, ""));
        if (Session.isLoggedIn()) {
            roleBadge.setText(Session.isSacco() ? "SACCO STAFF" : "MEMBER");
            roleBadge.setBackground(Session.isSacco() ? UiUtil.PRIMARY : UiUtil.ACCENT);
        } else {
            roleBadge.setText("NOT SIGNED IN");
            roleBadge.setBackground(UiUtil.TEXT_MUTED);
        }
    }

    private static String titleOf(String pageId) {
        for (String[] p : SACCO_PAGES) {
            if (p[0].equals(pageId)) {
                return p[1];
            }
        }
        for (String[] p : MEMBER_PAGES) {
            if (p[0].equals(pageId)) {
                return p[1];
            }
        }
        return pageId;
    }

    /** Navigates to Page 1 preloaded with a member for editing. */
    public void showMemberRegistration(Member m) {
        regPanel.loadMember(m);
        showPage("member-registration");
    }

    /** Navigates to Page 3 preloaded with a loan for editing. */
    public void showLoanEntry(LoanRecord loan) {
        loanEntry.loadLoan(loan);
        showPage("loan-entry");
    }

    /** Reloads data on every page that shows table content. */
    public void refreshAll() {
        dashboard.refresh();
        listMembers.refresh();
        loanList.refresh();
        savingsList.refresh();
        computeScore.refresh();
        scoreDisplay.refresh();
        generatePassport.refresh();
        savingsEntry.loadMembers();
        passportLog.refresh();
        loanEntry.loadMembers();
        loanRequests.refresh();
        updateStatus();
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        if (DBConnection.isConfigured()) {
            sb.append("Connected  |  ").append(DBConnection.getUrl());
        } else {
            sb.append("Not connected - use File > Connect");
        }
        if (Session.isLoggedIn()) {
            AppUser u = Session.user();
            sb.append("  |  ").append(u.getDisplayName())
              .append("  (").append(u.getRole()).append(")");
        }
        statusBar.setText(sb.toString());
    }

    /** Opens the connection dialog; returns true if a connection was established. */
    public boolean openConnectionDialog() {
        ConnectionDialog dlg = new ConnectionDialog(this);
        boolean ok = dlg.showAndGetConnected();
        if (ok && Session.isLoggedIn()) {
            refreshAll();
        }
        return ok;
    }

    /** Shows the login dialog; on success applies the session and returns true. */
    public boolean showLoginDialog() {
        LoginDialog dlg = new LoginDialog(this);
        AppUser u = dlg.showAndGetUser();
        if (u != null) {
            Session.setUser(u);
            applySession();
            return true;
        }
        return false;
    }

    /** Rebuilds navigation, reloads all data and shows the role's home page. */
    public void applySession() {
        rebuildNav();
        refreshAll();
        showPage("dashboard");
        System.out.println("[session] logged in as " + Session.username()
                + " role=" + Session.user().getRole());
    }

    private void logout() {
        Session.logout();
        if (!showLoginDialog()) {
            dispose();
        }
    }

    /** Convenience for tests: returns the content card layout. */
    public void goTo(String pageId) {
        showPage(pageId);
    }
}
