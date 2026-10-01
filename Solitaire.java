import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Solitaire extends JFrame {

    enum Suit {
        HEARTS("♥", Color.RED),
        DIAMONDS("♦", Color.RED),
        CLUBS("♣", Color.BLACK),
        SPADES("♠", Color.BLACK);

        final String symbol;
        final Color color;

        Suit(String symbol, Color color) {
            this.symbol = symbol;
            this.color = color;
        }
    }

    static class Card {
        final Suit suit;
        final int value; // 1 = Ass, 11 = Bube, 12 = Dame, 13 = König
        boolean faceUp;

        Card(Suit suit, int value) {
            this.suit = suit;
            this.value = value;
            this.faceUp = false;
        }

        String getValueString() {
            return switch (value) {
                case 1 -> "A";
                case 11 -> "J";
                case 12 -> "Q";
                case 13 -> "K";
                default -> String.valueOf(value);
            };
        }

        boolean isRed() {
            return suit == Suit.HEARTS || suit == Suit.DIAMONDS;
        }
    }

    public static class GamePanel extends JPanel {
        private static final int CARD_WIDTH = 75;
        private static final int CARD_HEIGHT = 105;
        private static final int CARD_OFFSET_Y = 20;

        // Spielbereiche
        private final List<Card> stock = new ArrayList<>(); // Nachziehstapel
        private final List<Card> waste = new ArrayList<>(); // Aufgedeckter Nachziehstapel
        private final List<List<Card>> foundations = new ArrayList<>(); // 4 Zielfelder (Ass bis König)
        private final List<List<Card>> tableau = new ArrayList<>(); // 7 Spalten

        // Drag & Drop Status
        private List<Card> draggedCards = null;
        private List<Card> dragSource = null;
        private Point dragOffset = new Point();
        private Point dragCurrentPoint = new Point();

        public GamePanel() {
            setBackground(new Color(34, 139, 34)); // Klassisches Filz-Grün
            initGame();

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleMousePressed(e.getPoint());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedCards != null) {
                        dragCurrentPoint = e.getPoint();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggedCards != null) {
                        handleMouseReleased(e.getPoint());
                    }
                }
            };

            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        public void initGame() {
            stock.clear();
            waste.clear();
            foundations.clear();
            tableau.clear();

            for (int i = 0; i < 4; i++)
                foundations.add(new ArrayList<>());
            for (int i = 0; i < 7; i++)
                tableau.add(new ArrayList<>());

            List<Card> deck = new ArrayList<>();
            for (Suit s : Suit.values()) {
                for (int v = 1; v <= 13; v++) {
                    deck.add(new Card(s, v));
                }
            }
            Collections.shuffle(deck);

            // 7 Spalten aufbauen
            for (int col = 0; col < 7; col++) {
                for (int row = 0; row <= col; row++) {
                    Card c = deck.remove(deck.size() - 1);
                    if (row == col)
                        c.faceUp = true;
                    tableau.get(col).add(c);
                }
            }

            // Rest bildet den Nachziehstapel
            stock.addAll(deck);
            repaint();
        }

        private void handleMousePressed(Point p) {
            // 1. Klick auf Nachziehstapel (Stock)
            Rectangle stockBounds = new Rectangle(30, 20, CARD_WIDTH, CARD_HEIGHT);
            if (stockBounds.contains(p)) {
                if (stock.isEmpty()) {
                    while (!waste.isEmpty()) {
                        Card c = waste.remove(waste.size() - 1);
                        c.faceUp = false;
                        stock.add(c);
                    }
                } else {
                    Card c = stock.remove(stock.size() - 1);
                    c.faceUp = true;
                    waste.add(c);
                }
                repaint();
                return;
            }

            // 2. Klick auf Waste-Stapel (oberste Karte ziehen)
            if (!waste.isEmpty()) {
                Rectangle wasteBounds = new Rectangle(120, 20, CARD_WIDTH, CARD_HEIGHT);
                if (wasteBounds.contains(p)) {
                    dragSource = waste;
                    draggedCards = new ArrayList<>();
                    draggedCards.add(waste.get(waste.size() - 1));
                    dragOffset.setLocation(p.x - 120, p.y - 20);
                    dragCurrentPoint = p;
                    repaint();
                    return;
                }
            }

            // 3. Klick auf Foundation-Stapel
            for (int i = 0; i < 4; i++) {
                List<Card> fList = foundations.get(i);
                if (!fList.isEmpty()) {
                    Rectangle fBounds = new Rectangle(300 + i * 95, 20, CARD_WIDTH, CARD_HEIGHT);
                    if (fBounds.contains(p)) {
                        dragSource = fList;
                        draggedCards = new ArrayList<>();
                        draggedCards.add(fList.get(fList.size() - 1));
                        dragOffset.setLocation(p.x - fBounds.x, p.y - fBounds.y);
                        dragCurrentPoint = p;
                        repaint();
                        return;
                    }
                }
            }

            // 4. Klick auf Tableau-Spalten
            for (int col = 0; col < 7; col++) {
                List<Card> tList = tableau.get(col);
                int x = 30 + col * 95;

                for (int row = tList.size() - 1; row >= 0; row--) {
                    int y = 150 + row * CARD_OFFSET_Y;
                    Rectangle cardBounds = (row == tList.size() - 1)
                            ? new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT)
                            : new Rectangle(x, y, CARD_WIDTH, CARD_OFFSET_Y);

                    if (cardBounds.contains(p)) {
                        Card clicked = tList.get(row);
                        if (!clicked.faceUp) {
                            // Oberste verdeckte Karte beim Anklicken aufdecken
                            if (row == tList.size() - 1) {
                                clicked.faceUp = true;
                                repaint();
                            }
                            return;
                        }

                        // Offene Kartenreihe ab der geklickten Karte greifen
                        dragSource = tList;
                        draggedCards = new ArrayList<>(tList.subList(row, tList.size()));
                        dragOffset.setLocation(p.x - x, p.y - y);
                        dragCurrentPoint = p;
                        repaint();
                        return;
                    }
                }
            }
        }

        private void handleMouseReleased(Point p) {
            boolean validMove = false;
            Card baseDragged = draggedCards.get(0);

            // A) Ablegen auf Foundation (nur Einzelkarten)
            if (draggedCards.size() == 1) {
                for (int i = 0; i < 4; i++) {
                    Rectangle fBounds = new Rectangle(300 + i * 95, 20, CARD_WIDTH, CARD_HEIGHT);
                    if (fBounds.contains(p)) {
                        List<Card> fList = foundations.get(i);
                        if (fList.isEmpty()) {
                            if (baseDragged.value == 1) { // Muss Ass sein
                                fList.add(baseDragged);
                                validMove = true;
                            }
                        } else {
                            Card top = fList.get(fList.size() - 1);
                            if (top.suit == baseDragged.suit && top.value + 1 == baseDragged.value) {
                                fList.add(baseDragged);
                                validMove = true;
                            }
                        }
                        break;
                    }
                }
            }

            // B) Ablegen auf Tableau-Spalten
            if (!validMove) {
                for (int col = 0; col < 7; col++) {
                    List<Card> tList = tableau.get(col);
                    int x = 30 + col * 95;
                    int y = tList.isEmpty() ? 150 : 150 + (tList.size() - 1) * CARD_OFFSET_Y;
                    Rectangle targetBounds = new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT);

                    if (targetBounds.intersects(
                            new Rectangle(p.x - dragOffset.x, p.y - dragOffset.y, CARD_WIDTH, CARD_HEIGHT))) {
                        if (tList.isEmpty()) {
                            if (baseDragged.value == 13) { // Nur König auf leere Spalte
                                tList.addAll(draggedCards);
                                validMove = true;
                                break;
                            }
                        } else {
                            Card top = tList.get(tList.size() - 1);
                            // Absteigender Wert und abwechselnde Farben (rot auf schwarz, schwarz auf rot)
                            if (top.faceUp && top.value == baseDragged.value + 1
                                    && top.isRed() != baseDragged.isRed()) {
                                tList.addAll(draggedCards);
                                validMove = true;
                                break;
                            }
                        }
                    }
                }
            }

            // Wenn Zug gültig war, Karten von der Quelle entfernen und ggf. Vorherige
            // aufdecken
            if (validMove) {
                dragSource.removeAll(draggedCards);
                if (!dragSource.isEmpty() && dragSource != waste) {
                    dragSource.get(dragSource.size() - 1).faceUp = true;
                }
                checkWin();
            }

            draggedCards = null;
            dragSource = null;
            repaint();
        }

        private void checkWin() {
            int total = 0;
            for (List<Card> f : foundations)
                total += f.size();
            if (total == 52) {
                JOptionPane.showMessageDialog(this, "Glückwunsch! Du hast das Spiel gewonnen!", "Sieg",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Nachziehstapel (Stock) zeichnen
            if (stock.isEmpty()) {
                drawEmptySlot(g2, 30, 20);
            } else {
                drawCardBack(g2, 30, 20);
            }

            // Waste-Stapel zeichnen
            if (waste.isEmpty()) {
                drawEmptySlot(g2, 120, 20);
            } else {
                Card c = waste.get(waste.size() - 1);
                if (draggedCards == null || !draggedCards.contains(c)) {
                    drawCard(g2, 120, 20, c);
                }
            }

            // 4 Zielfelder (Foundations) zeichnen
            for (int i = 0; i < 4; i++) {
                int x = 300 + i * 95;
                drawEmptySlot(g2, x, 20);
                List<Card> fList = foundations.get(i);
                if (!fList.isEmpty()) {
                    Card top = fList.get(fList.size() - 1);
                    if (draggedCards == null || !draggedCards.contains(top)) {
                        drawCard(g2, x, 20, top);
                    }
                }
            }

            // 7 Tableau-Spalten zeichnen
            for (int col = 0; col < 7; col++) {
                int x = 30 + col * 95;
                drawEmptySlot(g2, x, 150);
                List<Card> tList = tableau.get(col);

                for (int row = 0; row < tList.size(); row++) {
                    Card c = tList.get(row);
                    if (draggedCards != null && draggedCards.contains(c))
                        continue;

                    int y = 150 + row * CARD_OFFSET_Y;
                    if (c.faceUp) {
                        drawCard(g2, x, y, c);
                    } else {
                        drawCardBack(g2, x, y);
                    }
                }
            }

            // Gezogene Karten an Mausposition zeichnen
            if (draggedCards != null) {
                int x = dragCurrentPoint.x - dragOffset.x;
                int startY = dragCurrentPoint.y - dragOffset.y;
                for (int i = 0; i < draggedCards.size(); i++) {
                    drawCard(g2, x, startY + i * CARD_OFFSET_Y, draggedCards.get(i));
                }
            }
        }

        private void drawEmptySlot(Graphics2D g, int x, int y) {
            g.setColor(new Color(25, 105, 25));
            g.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 8, 8);
            g.setColor(new Color(20, 85, 20));
            g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 8, 8);
        }

        private void drawCardBack(Graphics2D g, int x, int y) {
            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 8, 8);
            g.setColor(new Color(30, 70, 150));
            g.fillRoundRect(x + 4, y + 4, CARD_WIDTH - 8, CARD_HEIGHT - 8, 6, 6);
            g.setColor(Color.BLACK);
            g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 8, 8);
        }

        private void drawCard(Graphics2D g, int x, int y, Card card) {
            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 8, 8);
            g.setColor(Color.BLACK);
            g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 8, 8);

            g.setColor(card.suit.color);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString(card.getValueString(), x + 6, y + 18);
            g.drawString(card.suit.symbol, x + 6, y + 34);

            g.setFont(new Font("SansSerif", Font.PLAIN, 28));
            FontMetrics fm = g.getFontMetrics();
            int sx = x + (CARD_WIDTH - fm.stringWidth(card.suit.symbol)) / 2;
            int sy = y + (CARD_HEIGHT / 2) + 10;
            g.drawString(card.suit.symbol, sx, sy);
        }
    }

    public Solitaire() {
        setTitle("Solitär (Klondike)");
        setSize(740, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel, BorderLayout.CENTER);

        JButton restartBtn = new JButton("Neues Spiel");
        restartBtn.addActionListener(e -> panel.initGame());
        JPanel controlPanel = new JPanel();
        controlPanel.add(restartBtn);
        add(controlPanel, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Solitaire().setVisible(true));
    }
}