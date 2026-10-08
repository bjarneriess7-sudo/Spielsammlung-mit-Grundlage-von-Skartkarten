import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RommeGui extends JFrame {

    private final UserAccount currentUser;
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

    public RommeGui(UserAccount user) {
        super("Rommé - Angemeldet als: " + (user != null ? user.getUsername() : "Gast"));
        this.currentUser = user;

        this.drawPile = new Deck();
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
            // 10 Karten für jeden Spieler austeilen
            for (int i = 0; i < 10; i++) {
                playerHand.add(drawPile.drawCard());
                botHand.add(drawPile.drawCard());
            }
            // Erste Karte auf den Ablagestapel
            discardPile.add(drawPile.drawCard());
        } catch (EmptyDeckException e) {
            JOptionPane.showMessageDialog(this, "Fehler beim Initialisieren des Kartendecks.");
        }
        sortHand(playerHand);
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(34, 112, 60));

        // Oberes Panel: Info und Menü-Rückkehr
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JButton backBtn = new JButton("Zurück zum Hauptmenü");
        backBtn.addActionListener(e -> {
            new MainMenu(currentUser).setVisible(true);
            dispose();
        });
        statusLabel = new JLabel("Willkommen! Ziehe eine Karte vom Deck oder Ablagestapel.", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        topPanel.add(backBtn, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Mittleres Panel: Stapel links und gemeldete Kombinationen rechts
        JPanel middleContainer = new JPanel(new BorderLayout(20, 20));
        middleContainer.setOpaque(false);
        middleContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Stapel (Nachzieh- & Ablagestapel)
        JPanel pilesPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        pilesPanel.setOpaque(false);
        drawDeckBtn = new JButton("Deck (" + drawPile.size() + ")");
        drawDeckBtn.setPreferredSize(new Dimension(130, 80));
        drawDeckBtn.setBackground(new Color(41, 128, 185));
        drawDeckBtn.setForeground(Color.WHITE);
        drawDeckBtn.addActionListener(e -> playerDrawFromDeck());

        drawDiscardBtn = new JButton(getDiscardPileTopText());
        drawDiscardBtn.setPreferredSize(new Dimension(130, 80));
        drawDiscardBtn.setBackground(new Color(230, 126, 34));
        drawDiscardBtn.setForeground(Color.WHITE);
        drawDiscardBtn.addActionListener(e -> playerDrawFromDiscard());

        pilesPanel.add(drawDeckBtn);
        pilesPanel.add(drawDiscardBtn);
        middleContainer.add(pilesPanel, BorderLayout.WEST);

        // Gemeldete Karten
        centerMeldsPanel = new JPanel();
        centerMeldsPanel.setLayout(new BoxLayout(centerMeldsPanel, BoxLayout.Y_AXIS));
        centerMeldsPanel.setBackground(new Color(24, 85, 45));
        JScrollPane meldScroll = new JScrollPane(centerMeldsPanel);
        meldScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE), "Tischablagen (Meldungen)", 0, 0, null, Color.WHITE));
        meldScroll.setOpaque(false);
        meldScroll.getViewport().setOpaque(false);
        middleContainer.add(meldScroll, BorderLayout.CENTER);

        add(middleContainer, BorderLayout.CENTER);

        // Unteres Panel: Handkarten und Aktionen
        JPanel bottomContainer = new JPanel(new BorderLayout(5, 5));
        bottomContainer.setOpaque(false);

        playerHandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        playerHandPanel.setOpaque(false);
        JScrollPane handScroll = new JScrollPane(playerHandPanel);
        handScroll.setPreferredSize(new Dimension(1000, 120));
        handScroll.setOpaque(false);
        handScroll.getViewport().setOpaque(false);
        bottomContainer.add(handScroll, BorderLayout.CENTER);

        // Aktionsleiste
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        actionsPanel.setOpaque(false);

        meldBtn = new JButton("Kombination melden");
        meldBtn.addActionListener(e -> playerMeldSelected());

        layOffBtn = new JButton("Anlegen");
        layOffBtn.addActionListener(e -> playerLayOffSelected());

        discardBtn = new JButton("Karte abwerfen (Zug beenden)");
        discardBtn.addActionListener(e -> playerDiscardSelected());

        actionsPanel.add(meldBtn);
        actionsPanel.add(layOffBtn);
        actionsPanel.add(discardBtn);
        bottomContainer.add(actionsPanel, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);

        refreshUI();
    }

    private String getDiscardPileTopText() {
        if (discardPile.isEmpty()) {
            return "Ablage (leer)";
        }
        Card top = discardPile.get(discardPile.size() - 1);
        return "Ablage: " + top.toString();
    }

    private void refreshUI() {
        // Hand aktualisieren
        playerHandPanel.removeAll();
        for (Card card : playerHand) {
            JButton cardBtn = new JButton(card.toString());
            cardBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
            if (selectedCards.contains(card)) {
                cardBtn.setBackground(new Color(241, 196, 15));
            } else {
                cardBtn.setBackground(Color.WHITE);
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

        // Meldungen aktualisieren
        centerMeldsPanel.removeAll();
        for (int i = 0; i < meldedSets.size(); i++) {
            List<Card> set = meldedSets.get(i);
            JPanel setRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            setRow.setOpaque(false);
            setRow.add(new JLabel("Gruppe " + (i + 1) + ": "));
            for (Card c : set) {
                JLabel cLbl = new JLabel("[" + c.toString() + "]");
                cLbl.setForeground(Color.WHITE);
                setRow.add(cLbl);
            }
            centerMeldsPanel.add(setRow);
        }

        drawDeckBtn.setText("Deck (" + drawPile.size() + ")");
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
            statusLabel.setText("Du hast " + drawn + " gezogen. Melde Karten oder wirf eine ab.");
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
        statusLabel.setText("Du hast " + picked + " vom Ablagestapel genommen.");
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
            statusLabel.setText("Kombination erfolgreich ausgelegt!");
            checkWinCondition(playerHand, "Herzlichen Glückwunsch! Du hast gewonnen!");
            refreshUI();
        } else {
            JOptionPane.showMessageDialog(this, "Ungültige Kombination! Erlaubt sind Sätze gleicher Ränge oder Sequenzen gleicher Farbe.");
        }
    }

    private void playerLayOffSelected() {
        if (selectedCards.size() != 1) {
            JOptionPane.showMessageDialog(this, "Wähle genau 1 Karte aus, um sie an eine Kombination anzulegen.");
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
                    statusLabel.setText("Karte erfolgreich angelegt!");
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
            JOptionPane.showMessageDialog(this, "Wähle genau 1 Karte zum Abwerfen aus.");
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
        statusLabel.setText("Computer ist am Zug...");

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
            // Bot zieht bevorzugt vom Deck
            Card drawn = drawPile.drawCard();
            botHand.add(drawn);
            sortHand(botHand);

            // Bot versucht Sequenz oder Satz zu finden
            findAndMeldForBot();

            // Bot wirft die erste Karte ab
            if (!botHand.isEmpty()) {
                Card discarded = botHand.remove(botHand.size() - 1);
                discardPile.add(discarded);
            }

            if (checkWinCondition(botHand, "Der Computer hat gewonnen!")) {
                return;
            }

            isPlayerTurn = true;
            statusLabel.setText("Du bist am Zug. Ziehe eine Karte.");
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
            new MainMenu(currentUser).setVisible(true);
            dispose();
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
            if (suits.contains(c.getSuit())) return false; // Keine doppelten Farben im Satz
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
}
