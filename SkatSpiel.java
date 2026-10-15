import java.util.*;

/**
 * Vollständige Skat-Implementierung (3 Spieler: Du, Bot 1, Bot 2).
 * Nutzt die gemeinsamen Klassen Card, Deck, Hand, Suit, Rank und GameCoreInterfaces.
 */
public class SkatGame implements GameCoreInterfaces {

    public enum GameType {
        FARBE, GRAND
    }

    public static class SkatGameMode {
        private final GameType type;
        private final Suit trumpSuit; // null bei Grand

        public SkatGameMode(GameType type, Suit trumpSuit) {
            this.type = type;
            this.trumpSuit = trumpSuit;
        }

        public GameType getType() { return type; }
        public Suit getTrumpSuit() { return trumpSuit; }
    }

    public static class SkatPlayer implements Player {
        private final String name;
        private final boolean isHuman;
        private final Hand hand;
        private final List<Card> tricksWon;
        private int score;

        public SkatPlayer(String name, boolean isHuman) {
            this.name = name;
            this.isHuman = isHuman;
            this.hand = new Hand();
            this.tricksWon = new ArrayList<>();
            this.score = 0;
        }

        @Override public String getName() { return name; }
        @Override public Hand getHand() { return hand; }
        @Override public int getScore() { return score; }
        @Override public void addScore(int points) { this.score += points; }
        @Override public boolean isHuman() { return isHuman; }

        public List<Card> getTricksWon() { return tricksWon; }

        public boolean isTrump(Card card, SkatGameMode mode) {
            if (card.getRank() == Rank.BUBE) return true;
            return mode.getTrumpSuit() != null && card.getSuit() == mode.getTrumpSuit();
        }

        public int getJackPower(Card card) {
            return switch (card.getSuit()) {
                case KARO -> 1;
                case HERZ -> 2;
                case PIK -> 3;
                case KREUZ -> 4;
            };
        }

        public int getSkatOrder(Card card) {
            return switch (card.getRank()) {
                case SIEBEN -> 0;
                case ACHT -> 1;
                case NEUN -> 2;
                case DAME -> 3;
                case KOENIG -> 4;
                case ZEHN -> 5;
                case ASS -> 6;
                case BUBE -> 7;
                default -> 0;
            };
        }

        public void sortHandForSkat(SkatGameMode mode) {
            hand.sortWithComparator((c1, c2) -> {
                boolean t1 = isTrump(c1, mode);
                boolean t2 = isTrump(c2, mode);

                if (t1 && !t2) return -1;
                if (!t1 && t2) return 1;

                if (t1 && t2) {
                    if (c1.getRank() == Rank.BUBE && c2.getRank() == Rank.BUBE) {
                        return Integer.compare(getJackPower(c2), getJackPower(c1));
                    }
                    if (c1.getRank() == Rank.BUBE) return -1;
                    if (c2.getRank() == Rank.BUBE) return 1;
                    return Integer.compare(getSkatOrder(c2), getSkatOrder(c1));
                }

                if (c1.getSuit() != c2.getSuit()) {
                    return Integer.compare(c2.getSuit().ordinal(), c1.getSuit().ordinal());
                }
                return Integer.compare(getSkatOrder(c2), getSkatOrder(c1));
            });
        }
    }

    private static final int[] BID_VALUES = {
            18, 20, 22, 23, 24, 27, 30, 33, 35, 36, 40, 44, 45, 46, 48, 50, 54, 55, 60
    };

    private final SkatPlayer[] players = new SkatPlayer[3];
    private final List<Card> skatCards = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);
    private boolean gameOver = false;
    private String outcome = "Noch nicht gespielt";

    public SkatGame() {
        players[0] = new SkatPlayer("Spieler (Du)", true);
        players[1] = new SkatPlayer("Mittelhand (Bot 1)", false);
        players[2] = new SkatPlayer("Hinterhand (Bot 2)", false);
    }

    @Override
    public void start() {
        Deck deck = new Deck();
        deck.initializeSkat32Deck();
        deck.shuffleFisherYates();

        for (SkatPlayer p : players) {
            p.getHand().clear();
            p.getTricksWon().clear();
        }
        skatCards.clear();

        try {
            for (SkatPlayer p : players) for (int i = 0; i < 3; i++) p.getHand().addCard(deck.drawCard());
            skatCards.add(deck.drawCard());
            skatCards.add(deck.drawCard());
            for (SkatPlayer p : players) for (int i = 0; i < 4; i++) p.getHand().addCard(deck.drawCard());
            for (SkatPlayer p : players) for (int i = 0; i < 3; i++) p.getHand().addCard(deck.drawCard());
        } catch (EmptyDeckException e) {
            throw new IllegalStateException("Skat-Deck unvollständig", e);
        }

        SkatGameMode neutral = new SkatGameMode(GameType.GRAND, null);
        for (SkatPlayer p : players) p.sortHandForSkat(neutral);

        System.out.println("\n=== OFFIZIELLES SKATSPIEL GESTARTET ===");
        printHand(players[0]);

        // Phase 1: Reizen
        int declarerIdx = runBidding();
        SkatPlayer declarer = players[declarerIdx];
        System.out.println("\n>> Alleinspieler ist: " + declarer.getName());

        // Phase 2: Skat drücken
        handleSkat(declarer);

        // Phase 3: Spielart wählen
        SkatGameMode mode = chooseGameMode(declarer);
        System.out.println("Angesagtes Spiel: " + (mode.getType() == GameType.GRAND ? "Grand" : mode.getTrumpSuit().getName()));

        for (SkatPlayer p : players) p.sortHandForSkat(mode);

        // Phase 4: Stiche spielen
        int leader = 0;
        for (int trick = 1; trick <= 10; trick++) {
            System.out.println("\n--- Stich " + trick + " von 10 ---");
            leader = playTrick(leader, mode);
        }

        // Phase 5: Auswertung
        evaluateGame(declarer);
        this.gameOver = true;
    }

    @Override
    public void processTurn() {}

    private int runBidding() {
        System.out.println("\n--- PHASE 1: DAS REIZEN ---");
        int winner = duelBidding(1, 0, 0);
        winner = duelBidding(2, winner, 0);
        return winner;
    }

    private int duelBidding(int askerIdx, int respIdx, int bidIdx) {
        SkatPlayer asker = players[askerIdx];
        SkatPlayer resp = players[respIdx];
        int cur = bidIdx;

        while (cur < BID_VALUES.length) {
            int val = BID_VALUES[cur];
            boolean ask = asker.isHuman() ? promptYesNo(asker.getName() + ": Reizen auf " + val + "?") : val <= 22;
            if (!ask) {
                System.out.println(asker.getName() + " passt.");
                return respIdx;
            }
            System.out.println(asker.getName() + " bietet " + val);

            boolean hold = resp.isHuman() ? promptYesNo(resp.getName() + ": Halten von " + val + "?") : val <= 20;
            if (!hold) {
                System.out.println(resp.getName() + " passt.");
                return askerIdx;
            }
            System.out.println(resp.getName() + " hält " + val);
            cur++;
        }
        return respIdx;
    }

    private void handleSkat(SkatPlayer declarer) {
        if (declarer.isHuman()) {
            System.out.println("\nDu nimmst den Skat auf:");
            for (Card c : skatCards) System.out.println("  + " + c);
            for (Card c : skatCards) declarer.getHand().addCard(c);
            skatCards.clear();

            for (int i = 0; i < 2; i++) {
                printHand(declarer);
                System.out.print("Wähle Karte " + (i + 1) + " zum Drücken in den Skat (1-" + declarer.getHand().size() + "): ");
                int choice = readIndex(declarer.getHand().size());
                skatCards.add(declarer.getHand().removeCard(choice));
            }
        } else {
            for (Card c : skatCards) declarer.getHand().addCard(c);
            skatCards.clear();
            declarer.getHand().sortWithComparator(Comparator.comparingInt(this::getCardEyes));
            skatCards.add(declarer.getHand().removeCard(0));
            skatCards.add(declarer.getHand().removeCard(0));
            System.out.println(declarer.getName() + " hat den Skat aufgenommen und verdeckt gedrückt.");
        }
    }

    private SkatGameMode chooseGameMode(SkatPlayer declarer) {
        if (declarer.isHuman()) {
            System.out.println("\nWähle Spielart: 1: Karo | 2: Herz | 3: Pik | 4: Kreuz | 5: Grand");
            int sel = readIndex(5) + 1;
            return switch (sel) {
                case 1 -> new SkatGameMode(GameType.FARBE, Suit.KARO);
                case 2 -> new SkatGameMode(GameType.FARBE, Suit.HERZ);
                case 3 -> new SkatGameMode(GameType.FARBE, Suit.PIK);
                case 4 -> new SkatGameMode(GameType.FARBE, Suit.KREUZ);
                default -> new SkatGameMode(GameType.GRAND, null);
            };
        }
        return new SkatGameMode(GameType.FARBE, Suit.KREUZ);
    }

    private int playTrick(int leaderIdx, SkatGameMode mode) {
        Card[] trick = new Card[3];
        int cur = leaderIdx;

        for (int i = 0; i < 3; i++) {
            SkatPlayer p = players[cur];
            Card chosen;
            if (p.isHuman()) {
                printHand(p);
                while (true) {
                    System.out.print("Karte legen (1-" + p.getHand().size() + "): ");
                    int idx = readIndex(p.getHand().size());
                    Card c = p.getHand().getCard(idx);
                    if (isValidMove(p, c, trick[leaderIdx], mode)) {
                        chosen = p.getHand().removeCard(idx);
                        break;
                    }
                    System.out.println("Ungültig! Farbbedienpflicht beachten.");
                }
            } else {
                chosen = botChooseCard(p, trick[leaderIdx], mode);
            }
            trick[cur] = chosen;
            System.out.println(p.getName() + " legt: " + chosen);
            cur = (cur + 1) % 3;
        }

        int winnerIdx = evalTrick(trick, leaderIdx, mode);
        System.out.println("-> " + players[winnerIdx].getName() + " gewinnt den Stich!");
        for (Card c : trick) players[winnerIdx].getTricksWon().add(c);
        return winnerIdx;
    }

    private Card botChooseCard(SkatPlayer p, Card lead, SkatGameMode mode) {
        for (int i = 0; i < p.getHand().size(); i++) {
            Card c = p.getHand().getCard(i);
            if (isValidMove(p, c, lead, mode)) {
                return p.getHand().removeCard(i);
            }
        }
        return p.getHand().removeCard(0);
    }

    private boolean isValidMove(SkatPlayer p, Card cand, Card lead, SkatGameMode mode) {
        if (lead == null) return true;
        boolean leadTrump = p.isTrump(lead, mode);
        boolean candTrump = p.isTrump(cand, mode);

        if (leadTrump) {
            boolean hasTrump = p.getHand().getCards().stream().anyMatch(c -> p.isTrump(c, mode));
            return !hasTrump || candTrump;
        } else {
            boolean hasSuit = p.getHand().getCards().stream().anyMatch(c -> !p.isTrump(c, mode) && c.getSuit() == lead.getSuit());
            if (hasSuit) {
                return !candTrump && cand.getSuit() == lead.getSuit();
            }
            return true;
        }
    }

    private int evalTrick(Card[] trick, int leaderIdx, SkatGameMode mode) {
        int best = leaderIdx;
        for (int i = 1; i < 3; i++) {
            int ch = (leaderIdx + i) % 3;
            if (isHigher(trick[ch], trick[best], mode)) {
                best = ch;
            }
        }
        return best;
    }

    private boolean isHigher(Card cand, Card cur, SkatGameMode mode) {
        SkatPlayer dummy = players[0];
        boolean cT = dummy.isTrump(cand, mode);
        boolean bT = dummy.isTrump(cur, mode);

        if (cT && !bT) return true;
        if (!cT && bT) return false;

        if (cT && bT) {
            if (cand.getRank() == Rank.BUBE && cur.getRank() == Rank.BUBE) {
                return dummy.getJackPower(cand) > dummy.getJackPower(cur);
            }
            if (cand.getRank() == Rank.BUBE) return true;
            if (cur.getRank() == Rank.BUBE) return false;
            return dummy.getSkatOrder(cand) > dummy.getSkatOrder(cur);
        }

        if (cand.getSuit() == cur.getSuit()) {
            return dummy.getSkatOrder(cand) > dummy.getSkatOrder(cur);
        }
        return false;
    }

    private void evaluateGame(SkatPlayer declarer) {
        declarer.getTricksWon().addAll(skatCards);
        int declarerEyes = declarer.getTricksWon().stream().mapToInt(this::getCardEyes).sum();
        int defenderEyes = 120 - declarerEyes;

        System.out.println("\n=================================");
        System.out.println("SKAT-ENDABRECHNUNG:");
        System.out.println(declarer.getName() + " (Alleinspieler): " + declarerEyes + " Augen");
        System.out.println("Verteidiger: " + defenderEyes + " Augen");

        if (declarerEyes >= 61) {
            outcome = "Alleinspieler " + declarer.getName() + " hat GEWONNEN (" + declarerEyes + " Augen)!";
            declarer.addScore(50);
        } else {
            outcome = "Alleinspieler " + declarer.getName() + " hat VERLOREN (" + declarerEyes + " Augen)!";
        }
        System.out.println("-> " + outcome);
        System.out.println("=================================");
    }

    public int getCardEyes(Card c) {
        return switch (c.getRank()) {
            case ASS -> 11;
            case ZEHN -> 10;
            case KOENIG -> 4;
            case DAME -> 3;
            case BUBE -> 2;
            default -> 0;
        };
    }

    private void printHand(SkatPlayer p) {
        System.out.println("\nDeine Handkarten:");
        for (int i = 0; i < p.getHand().size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + p.getHand().getCard(i));
        }
    }

    private int readIndex(int max) {
        while (true) {
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= 1 && val <= max) return val - 1;
            } catch (Exception ignored) {}
            System.out.print("Bitte Zahl von 1 bis " + max + " eingeben: ");
        }
    }

    private boolean promptYesNo(String msg) {
        System.out.print(msg + " (j/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("j");
    }

    @Override public boolean isGameOver() { return gameOver; }
    @Override public String getResultSummary() { return outcome; }

    public static void main(String[] args) {
        new SkatGame().start();
    }
}
