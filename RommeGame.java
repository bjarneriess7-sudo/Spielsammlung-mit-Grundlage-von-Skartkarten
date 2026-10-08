import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RommeGui extends JFrame {

    private final UserAccount currentUser;
    private final JFrame parentMenu;
    private final Deck drawPile;
    private final List<Card> discardPile;

    private final List<Card> playerHand;
    private final List<Card> botHand;
    private final List<List<Card>> meldedSets;

    private final List<Card> selectedCards;

    private boolean isPlayerTurn;
    private boolean hasDrawnThisTurn;

    // GUI Komponenten
    private JPanel centerMeldsPanel;
    private JPanel playerHandPanel;
    private JLabel statusLabel;
    private JButton drawDeckBtn;
    private JButton drawDiscardBtn;
    private JButton meldBtn;
    private JButton layOffBtn;
    private JButton discardBtn;

    public RommeGui(JFrame parentMenu) {
        this(parentMenu, null);
    }

    public RommeGui(UserAccount user) {
        this(null, user);
    }

    public RommeGui() {
        this(null, null);
    }

    public RommeGui(JFrame parentMenu, UserAccount user) {
        super("Rommé - Angemeldet als: " + (user != null ? user.getUsername() : "Gast"));
        this.parentMenu = parentMenu;
        this.currentUser = user;

        this.drawPile = new Deck();
        this.drawPile.initializeStandard52Deck();
        this.drawPile.shuffle();
        this.discardPile = new ArrayList<>();
        this.playerHand = new ArrayList<>();
        this.botHand = new ArrayList<>();
        this.meldedSets = new ArrayList<>();
        this.selectedCards = new ArrayList<>();

        this.isPlayerTurn = true;
        this.hasDrawnThisTurn = false;

        initGame();
        initUI();
    }

    private void initGame() {
        try {
            for (int i = 0; i < 10; i++) {
                playerHand.add(drawPile.drawCard());
                botHand.add(drawPile.drawCard());
            }
            discardPile.add(drawPile.drawCard());
        } catch (EmptyDeckException e) {
            JOptionPane.showMessageDialog(this, "Fehler beim Initialisieren des Decks.");
        }
        sortHand(playerHand);
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(new Color(24, 105, 52));

        // 1. Top Panel (Menüleiste & Status)
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(12, 20, 10, 20));

        JButton backBtn = new JButton("⬅ Zurück zum Hauptmenü");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        backBtn.setBackground(new Color(40, 48, 62));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> returnToMenu());

        statusLabel = new JLabel("Schritt 1: Ziehe eine Karte vom Nachzieh- oder Ablagestapel!", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(255, 235, 120));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        topPanel.add(backBtn, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // 2. Middle Panel (Feste Stapel-Box links + Meldungsbereich rechts)
        JPanel middleContainer = new JPanel(new BorderLayout(25, 0));
        middleContainer.setOpaque(false);
        middleContainer.setBorder(new EmptyBorder(10, 25, 10, 25));

        // Stapel in fixer Größe (kein vertikales Verzerren)
        JPanel pilesWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
        pilesWrapper.setOpaque(false);
        pilesWrapper.setPreferredSize(new Dimension(170, 360));

        JPanel pilesBox = new JPanel(new GridLayout(2, 1, 0, 20));
        pilesBox.setOpaque(false);

        drawDeckBtn = createCardPileButton("Deck", new Color(30, 70, 140));
        drawDeckBtn.addActionListener(e -> playerDrawFromDeck());

        drawDiscardBtn = createCardPileButton("Ablage", new Color(190, 80, 25));
        drawDiscardBtn.addActionListener(e -> playerDrawFromDiscard());

        pilesBox.add(drawDeckBtn);
        pilesBox.add(drawDiscardBtn);
        pilesWrapper.add(pilesBox);
        middleContainer.add(pilesWrapper, BorderLayout.WEST);

        // Auslagefeld
        centerMeldsPanel = new JPanel();
        centerMeldsPanel.setLayout(new BoxLayout(centerMeldsPanel, BoxLayout.Y_AXIS));
        centerMeldsPanel.setBackground(new Color(18, 75, 38));

        JScrollPane meldScroll = new JScrollPane(centerMeldsPanel);
        meldScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 180), 2),
                " Tischablagen / Meldungen (mind. 30 Punkte) ",
                0, 0, new Font("SansSerif", Font.BOLD, 14), Color.WHITE));
        meldScroll.setOpaque(false);
        meldScroll.getViewport().setOpaque(false);
        middleContainer.add(meldScroll, BorderLayout.CENTER);

        add(middleContainer, BorderLayout.CENTER);

        // 3. Bottom Panel (Handkarten und Aktionsleiste)
        JPanel bottomContainer = new JPanel(new BorderLayout(0, 10));
        bottomContainer.setOpaque(false);
        bottomContainer.setBorder(new EmptyBorder(5, 20, 20, 20));

        playerHandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        playerHandPanel.setOpaque(false);

        JScrollPane handScroll = new JScrollPane(playerHandPanel);
        handScroll.setPreferredSize(new Dimension(1100, 155));
        handScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 120), 1),
                " Deine Handkarten ", 0, 0, new Font("SansSerif", Font.BOLD, 13), Color.WHITE));
        handScroll.setOpaque(false);
        handScroll.getViewport().setOpaque(false);
        bottomContainer.add(handScroll, BorderLayout.CENTER);

        // Buttons
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        actionsPanel.setOpaque(false);

        meldBtn = createActionButton("Kombination melden (mind. 3)");
        meldBtn.addActionListener(e -> playerMeldSelected());

        layOffBtn = createActionButton("Anlegen (1 Karte)");
        layOffBtn.addActionListener(e -> playerLayOffSelected());

        discardBtn = createActionButton("Karte abwerfen (Zug beenden)");
        discardBtn.addActionListener(e -> playerDiscardSelected());

        actionsPanel.add(meldBtn);
        actionsPanel.add(layOffBtn);
        actionsPanel.add(discardBtn);
        bottomContainer.add(actionsPanel, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                returnToMenu();
            }
        });

        refreshUI();
    }

    private JButton createCardPileButton(String label, Color bg) {
        JButton btn = new JButton(label);
        btn.setPreferredSize(new Dimension(135, 150));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        return btn;
    }

    private JButton createActionButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(new Color(45, 55, 75));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(240, 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void returnToMenu() {
        dispose();
        if (parentMenu != null) {
            parentMenu.setVisible(true);
        } else {
            new MainMenu().setVisible(true);
        }
    }

    private String getDiscardPileTopText() {
        if (discardPile.isEmpty()) {
            return "<html><center>Ablage<br>(leer)</center></html>";
        }
        Card top = discardPile.get(discardPile.size() - 1);
        return "<html><center>Ablage<br><b>" + top.getRank().getShortName() + " " + top.getSuit().getSymbol() + "</b></center></html>";
    }

    private void refreshUI() {
        // Handkarten aktualisieren
        playerHandPanel.removeAll();
        for (Card card : playerHand) {
            JButton cardBtn = new JButton();
            cardBtn.setLayout(new BorderLayout());
            cardBtn.setPreferredSize(new Dimension(85, 125));
            cardBtn.setFocusPainted(false);
            cardBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            boolean isRed = card.getSuit() == Suit.HERZ || card.getSuit() == Suit.KARO;
            Color cardColor = isRed ? new Color(195, 20, 20) : Color.BLACK;

            JLabel topLabel = new JLabel(" " + card.getRank().getShortName() + " " + card.getSuit().getSymbol());
            topLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            topLabel.setForeground(cardColor);

            JLabel centerLabel = new JLabel(card.getSuit().getSymbol(), SwingConstants.CENTER);
            centerLabel.setFont(new Font("SansSerif", Font.PLAIN, 32));
            centerLabel.setForeground(cardColor);

            cardBtn.add(topLabel, BorderLayout.NORTH);
            cardBtn.add(centerLabel, BorderLayout.CENTER);

            if (selectedCards.contains(card)) {
                cardBtn.setBackground(new Color(255, 225, 110));
                cardBtn.setBorder(BorderFactory.createLineBorder(new Color(210, 140, 0), 3));
            } else {
                cardBtn.setBackground(Color.WHITE);
                cardBtn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            }

            cardBtn.addActionListener(e -> {
                if (selectedCards.contains(card)) {
                    selectedCards.remove(card);
                } else {
                    selectedCards.add(card);
                }
                refreshUI();
            });
            playerHandPanel.add(cardBtn);
        }

        // Auslagen aktualisieren
        centerMeldsPanel.removeAll();
        for (int i = 0; i < meldedSets.size(); i++) {
            List<Card> set = meldedSets.get(i);
            JPanel setRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            setRow.setOpaque(false);

            JLabel titleLbl = new JLabel("Gruppe " + (i + 1) + ": ");
            titleLbl.setForeground(new Color(255, 235, 130));
            titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
            setRow.add(titleLbl);

            for (Card c : set) {
                JLabel cLbl = new JLabel(" " + c.getRank().getShortName() + c.getSuit().getSymbol() + " ");
                cLbl.setOpaque(true);
                cLbl.setBackground(Color.WHITE);
                cLbl.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                boolean isRed = c.getSuit() == Suit.HERZ || c.getSuit() == Suit.KARO;
                cLbl.setForeground(isRed ? Color.RED : Color.BLACK);
                cLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
                setRow.add(cLbl);
            }
            centerMeldsPanel.add(setRow);
        }

        drawDeckBtn.setText("<html><center>Deck<br>(" + drawPile.size() + " Karten)</center></html>");
        drawDiscardBtn.setText(getDiscardPileTopText());

        drawDeckBtn.setEnabled(isPlayerTurn && !hasDrawnThisTurn);
        drawDiscardBtn.setEnabled(isPlayerTurn && !hasDrawnThisTurn && !discardPile.isEmpty());

        meldBtn.setEnabled(isPlayerTurn && hasDrawnThisTurn);
        layOffBtn.setEnabled(isPlayerTurn && hasDrawnThisTurn);
        discardBtn.setEnabled(isPlayerTurn && hasDrawnThisTurn);

        revalidate();
        repaint();
    }

    private void playerDrawFromDeck() {
        if (!isPlayerTurn || hasDrawnThisTurn) return;
        try {
            ensureDeckNotEmpty();
            Card drawn = drawPile.drawCard();
            playerHand.add(drawn);
            sortHand(playerHand);
            hasDrawnThisTurn = true;
            statusLabel.setText("Schritt 2: Karte (" + drawn + ") gezogen. Wähle Karten zum Melden oder Abwerfen!");
            refreshUI();
        } catch (EmptyDeckException e) {
            statusLabel.setText("Das Deck ist komplett leer!");
        }
    }

    private void playerDrawFromDiscard() {
        if (!isPlayerTurn || hasDrawnThisTurn || discardPile.isEmpty()) return;
        Card picked = discardPile.remove(discardPile.size() - 1);
        playerHand.add(picked);
        sortHand(playerHand);
        hasDrawnThisTurn = true;
        statusLabel.setText("Schritt 2: (" + picked + ") aufgenommen. Wähle Karten zum Melden oder Abwerfen!");
        refreshUI();
    }

    private void playerMeldSelected() {
        if (selectedCards.size() < 3) {
            JOptionPane.showMessageDialog(this, "Eine Meldung benötigt mindestens 3 Karten.");
            return;
        }
        if (isValidMeld(selectedCards)) {
            List<Card> newMeld = new ArrayList<>(selectedCards);
            sortMeld(newMeld);
            meldedSets.add(newMeld);
            playerHand.removeAll(selectedCards);
            selectedCards.clear();
            statusLabel.setText("Kombination erfolgreich ausgelegt! Wirf nun eine Karte ab.");
            checkWinCondition(playerHand, "Herzlichen Glückwunsch! Du hast gewonnen!");
            refreshUI();
        } else {
            JOptionPane.showMessageDialog(this, "Ungültige Kombination! Erlaubt sind Sätze gleicher Ränge oder Sequenzen gleicher Farbe.");
        }
    }

    private void playerLayOffSelected() {
        if (selectedCards.size() != 1) {
            JOptionPane.showMessageDialog(this, "Wähle genau 1 Karte aus, um sie anzulegen.");
            return;
        }
        if (meldedSets.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Es gibt noch keine Meldungen auf dem Tisch.");
            return;
        }

        String[] options = new String[meldedSets.size()];
        for (int i = 0; i < meldedSets.size(); i++) {
            options[i] = "Gruppe " + (i + 1) + ": " + meldedSets.get(i).toString();
        }

        String selectedGroup = (String) JOptionPane.showInputDialog(
                this, "Wähle die Gruppe zum Anlegen:", "Anlegen",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (selectedGroup != null) {
            int index = -1;
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(selectedGroup)) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                Card toAdd = selectedCards.get(0);
                List<Card> targetSet = meldedSets.get(index);
                List<Card> testSet = new ArrayList<>(targetSet);
                testSet.add(toAdd);

                if (isValidMeld(testSet)) {
                    sortMeld(testSet);
                    targetSet.clear();
                    targetSet.addAll(testSet);
                    playerHand.remove(toAdd);
                    selectedCards.clear();
                    statusLabel.setText("Karte erfolgreich angelegt! Wirf nun eine Karte ab.");
                    checkWinCondition(playerHand, "Herzlichen Glückwunsch! Du hast gewonnen!");
                    refreshUI();
                } else {
                    JOptionPane.showMessageDialog(this, "Die Karte passt nicht an diese Kombination.");
                }
            }
        }
    }

    private void playerDiscardSelected() {
        if (selectedCards.size() != 1) {
            JOptionPane.showMessageDialog(this, "Klicke genau 1 Handkarte gelb an, um sie abzuwerfen.");
            return;
        }
        Card toDiscard = selectedCards.get(0);
        playerHand.remove(toDiscard);
        selectedCards.clear();
        discardPile.add(toDiscard);

        if (checkWinCondition(playerHand, "Herzlichen Glückwunsch! Du hast gewonnen!")) {
            return;
        }

        isPlayerTurn = false;
        hasDrawnThisTurn = false;
        refreshUI();
        statusLabel.setText("Computer überlegt und zieht...");

        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                botTurn();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void botTurn() {
        try {
            ensureDeckNotEmpty();
            Card drawn = drawPile.drawCard();
            botHand.add(drawn);
            sortHand(botHand);

            findAndMeldForBot();

            if (!botHand.isEmpty()) {
                Card discarded = botHand.remove(botHand.size() - 1);
                discardPile.add(discarded);
            }

            if (checkWinCondition(botHand, "Der Computer hat gewonnen!")) {
                return;
            }

            isPlayerTurn = true;
            statusLabel.setText("Du bist wieder am Zug! Klicke auf das Deck oder die Ablage.");
            refreshUI();
        } catch (EmptyDeckException ex) {
            statusLabel.setText("Das Spiel endet unentschieden (keine Karten mehr).");
        }
    }

    private void findAndMeldForBot() {
        for (int i = 0; i < botHand.size() - 2; i++) {
            for (int j = i + 1; j < botHand.size() - 1; j++) {
                for (int k = j + 1; k < botHand.size(); k++) {
                    List<Card> candidate = new ArrayList<>();
                    candidate.add(botHand.get(i));
                    candidate.add(botHand.get(j));
                    candidate.add(botHand.get(k));
                    if (isValidMeld(candidate)) {
                        sortMeld(candidate);
                        meldedSets.add(candidate);
                        botHand.remove(k);
                        botHand.remove(j);
                        botHand.remove(i);
                        return;
                    }
                }
            }
        }
    }

    private boolean checkWinCondition(List<Card> hand, String message) {
        if (hand.isEmpty()) {
            refreshUI();
            JOptionPane.showMessageDialog(this, message, "Spiel beendet", JOptionPane.INFORMATION_MESSAGE);
            returnToMenu();
            return true;
        }
        return false;
    }

    private void ensureDeckNotEmpty() throws EmptyDeckException {
        if (drawPile.isEmpty()) {
            if (discardPile.size() > 1) {
                Card top = discardPile.remove(discardPile.size() - 1);
                while (!discardPile.isEmpty()) {
                    drawPile.addCard(discardPile.remove(0));
                }
                drawPile.shuffle();
                discardPile.add(top);
            } else {
                throw new EmptyDeckException("Keine Karten mehr verfügbar.");
            }
        }
    }

    private boolean isValidMeld(List<Card> cards) {
        if (cards == null || cards.size() < 3) return false;
        return isSet(cards) || isSequence(cards);
    }

    private boolean isSet(List<Card> cards) {
        Rank firstRank = cards.get(0).getRank();
        List<Suit> suits = new ArrayList<>();
        for (Card c : cards) {
            if (c.getRank() != firstRank) return false;
            if (suits.contains(c.getSuit())) return false;
            suits.add(c.getSuit());
        }
        return true;
    }

    private boolean isSequence(List<Card> cards) {
        List<Card> sorted = new ArrayList<>(cards);
        sortMeld(sorted);

        Suit suit = sorted.get(0).getSuit();
        for (int i = 0; i < sorted.size() - 1; i++) {
            Card c1 = sorted.get(i);
            Card c2 = sorted.get(i + 1);
            if (c1.getSuit() != suit || c2.getSuit() != suit) return false;
            if (c2.getRank().ordinal() != c1.getRank().ordinal() + 1) return false;
        }
        return true;
    }

    private void sortMeld(List<Card> cards) {
        cards.sort(Comparator.comparingInt(c -> c.getRank().ordinal()));
    }

    private void sortHand(List<Card> hand) {
        hand.sort((c1, c2) -> {
            int suitComp = c1.getSuit().name().compareTo(c2.getSuit().name());
            if (suitComp != 0) return suitComp;
            return Integer.compare(c1.getRank().ordinal(), c2.getRank().ordinal());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RommeGui().setVisible(true));
    }
}
