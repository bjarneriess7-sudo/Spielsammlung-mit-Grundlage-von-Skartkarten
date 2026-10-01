import java.util.*;

public class SkatSpiel {

    public enum Suit {
        KARO("Karo (♦)", 9),
        HERZ("Herz (♥)", 10),
        PIK("Pik (♠)", 11),
        KREUZ("Kreuz (♣)", 12);

        final String name;
        final int multiplier;

        Suit(String name, int multiplier) {
            this.name = name;
            this.multiplier = multiplier;
        }
    }

    public enum Rank {
        SIEBEN("7", 0, 0),
        ACHT("8", 1, 0),
        NEUN("9", 2, 0),
        DAME("Dame", 3, 3),
        KOENIG("König", 4, 4),
        ZEHN("10", 5, 10),
        ASS("Ass", 6, 11),
        BUBE("Bube", 7, 2);

        final String label;
        final int orderInSuit;
        final int points;

        Rank(String label, int orderInSuit, int points) {
            this.label = label;
            this.orderInSuit = orderInSuit;
            this.points = points;
        }
    }

    public static class Card {
        final Suit suit;
        final Rank rank;

        Card(Suit suit, Rank rank) {
            this.suit = suit;
            this.rank = rank;
        }

        public boolean isJack() {
            return rank == Rank.BUBE;
        }

        public boolean isTrump(GameMode mode) {
            if (isJack())
                return true;
            return mode.trumpSuit != null && mode.trumpSuit == this.suit;
        }

        public int getJackPower() {
            return switch (this.suit) {
                case KARO -> 1;
                case HERZ -> 2;
                case PIK -> 3;
                case KREUZ -> 4;
            };
        }

        @Override
        public String toString() {
            return suit.name + " " + rank.label;
        }
    }

    public enum GameType {
        FARBE, GRAND
    }

    public static class GameMode {
        final GameType type;
        final Suit trumpSuit; // null bei Grand

        GameMode(GameType type, Suit trumpSuit) {
            this.type = type;
            this.trumpSuit = trumpSuit;
        }
    }

    public static class Player {
        final String name;
        final boolean isHuman;
        final List<Card> hand = new ArrayList<>();
        final List<Card> tricksWon = new ArrayList<>();

        Player(String name, boolean isHuman) {
            this.name = name;
            this.isHuman = isHuman;
        }

        public void sortHand(GameMode mode) {
            hand.sort((c1, c2) -> {
                boolean t1 = c1.isTrump(mode);
                boolean t2 = c2.isTrump(mode);

                if (t1 && !t2)
                    return -1;
                if (!t1 && t2)
                    return 1;

                if (t1 && t2) {
                    if (c1.isJack() && c2.isJack()) {
                        return Integer.compare(c2.getJackPower(), c1.getJackPower());
                    }
                    if (c1.isJack())
                        return -1;
                    if (c2.isJack())
                        return 1;
                    return Integer.compare(c2.rank.orderInSuit, c1.rank.orderInSuit);
                }

                if (c1.suit != c2.suit) {
                    return Integer.compare(c2.suit.ordinal(), c1.suit.ordinal());
                }
                return Integer.compare(c2.rank.orderInSuit, c1.rank.orderInSuit);
            });
        }
    }

    private static final int[] BID_VALUES = {
            18, 20, 22, 23, 24, 27, 30, 33, 35, 36, 40, 44, 45, 46, 48, 50, 54, 55, 60
    };

    private final Player[] players = new Player[3];
    private final List<Card> skat = new ArrayList<>();
    private final Scanner scanner = new Scanner(System.in);

    public SkatSpiel() {
        players[0] = new Player("Du", true);
        players[1] = new Player("Mittelhand (Bot 1)", false);
        players[2] = new Player("Hinterhand (Bot 2)", false);
    }

    public void start() {
        dealCards();
        System.out.println("=== WILLKOMMEN BEIM SKAT ===");
        printHand(players[0]);

        // Phase 1: Reizen
        int declarerIndex = runBidding();
        Player declarer = players[declarerIndex];
        System.out.println("\n-> Alleinspieler ist: " + declarer.name);

        // Phase 2: Skat aufnehmen & drücken
        handleSkat(declarer);

        // Phase 3: Spielansage
        GameMode mode = chooseGameMode(declarer);
        System.out.println("\nAngesagtes Spiel: " + (mode.type == GameType.GRAND ? "Grand" : mode.trumpSuit.name));

        for (Player p : players)
            p.sortHand(mode);

        // Phase 4: Stiche spielen (10 Runden)
        int leader = 0; // Vorhand spielt zum 1. Stich aus
        for (int trick = 1; trick <= 10; trick++) {
            System.out.println("\n--- Stich " + trick + " von 10 ---");
            leader = playTrick(leader, mode);
        }

        // Phase 5: Auswertung
        evaluateGame(declarer);
    }

    private void dealCards() {
        List<Card> deck = new ArrayList<>();
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                deck.add(new Card(s, r));
            }
        }
        Collections.shuffle(deck);

        int idx = 0;
        // 3 Karten an jeden
        for (Player p : players)
            for (int i = 0; i < 3; i++)
                p.hand.add(deck.get(idx++));
        // 2 in den Skat
        skat.add(deck.get(idx++));
        skat.add(deck.get(idx++));
        // 4 Karten an jeden
        for (Player p : players)
            for (int i = 0; i < 4; i++)
                p.hand.add(deck.get(idx++));
        // 3 Karten an jeden
        for (Player p : players)
            for (int i = 0; i < 3; i++)
                p.hand.add(deck.get(idx++));

        GameMode neutralMode = new GameMode(GameType.GRAND, null);
        for (Player p : players)
            p.sortHand(neutralMode);
    }

    private int runBidding() {
        System.out.println("\n--- REIZEN ---");
        int winner = 0; // Standardmäßig gewinnt Vorhand, wenn alle passen
        int bidIndex = 0;

        // Runde 1: Mittelhand fragt, Vorhand antwortet
        winner = duelBidding(1, 0, bidIndex);

        // Runde 2: Hinterhand fragt Sieger aus Runde 1
        int challenger = 2;
        if (winner == challenger)
            challenger = 1; // Falls Hinterhand schon dran war
        winner = duelBidding(2, winner, bidIndex);

        return winner;
    }

    private int duelBidding(int askerIdx, int responderIdx, int startBidIdx) {
        Player asker = players[askerIdx];
        Player responder = players[responderIdx];
        int currentBidIdx = startBidIdx;

        while (currentBidIdx < BID_VALUES.length) {
            int value = BID_VALUES[currentBidIdx];

            // Frager entscheidet zu bieten
            boolean askerWants = askBid(asker, value);
            if (!askerWants) {
                System.out.println(asker.name + " passt.");
                return responderIdx;
            }
            System.out.println(asker.name + " reizt " + value);

            // Antwortender entscheidet zu halten
            boolean responderHolds = holdBid(responder, value);
            if (!responderHolds) {
                System.out.println(responder.name + " passt.");
                return askerIdx;
            }
            System.out.println(responder.name + " hält " + value);

            currentBidIdx++;
        }
        return responderIdx;
    }

    private boolean askBid(Player p, int value) {
        if (!p.isHuman)
            return value <= 22; // Einfache Bot-Schwelle
        System.out.print("Reizen auf " + value + "? (j/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("j");
    }

    private boolean holdBid(Player p, int value) {
        if (!p.isHuman)
            return value <= 20;
        System.out.print("Möchtest du " + value + " halten? (j/n): ");
        return scanner.nextLine().trim().equalsIgnoreCase("j");
    }

    private void handleSkat(Player declarer) {
        if (declarer.isHuman) {
            System.out.println("\nDu nimmst den Skat auf:");
            for (Card c : skat)
                System.out.println("  + " + c);
            declarer.hand.addAll(skat);
            skat.clear();

            for (int i = 0; i < 2; i++) {
                printIndexedHand(declarer);
                System.out.print("Wähle Karte " + (i + 1) + " zum Drücken in den Skat (Nummer): ");
                int choice = readCardIndex(declarer.hand.size());
                skat.add(declarer.hand.remove(choice));
            }
        } else {
            // Bot legt die zwei schwächsten/punktärmsten Karten
            declarer.hand.addAll(skat);
            skat.clear();
            declarer.hand.sort(Comparator.comparingInt(c -> c.rank.points));
            skat.add(declarer.hand.remove(0));
            skat.add(declarer.hand.remove(0));
            System.out.println(declarer.name + " hat den Skat aufgenommen und gedrückt.");
        }
    }

    private GameMode chooseGameMode(Player declarer) {
        if (declarer.isHuman) {
            System.out.println("\nWähle die Spielart:");
            System.out.println("1: Karo | 2: Herz | 3: Pik | 4: Kreuz | 5: Grand");
            while (true) {
                System.out.print("Auswahl (1-5): ");
                String input = scanner.nextLine().trim();
                switch (input) {
                    case "1":
                        return new GameMode(GameType.FARBE, Suit.KARO);
                    case "2":
                        return new GameMode(GameType.FARBE, Suit.HERZ);
                    case "3":
                        return new GameMode(GameType.FARBE, Suit.PIK);
                    case "4":
                        return new GameMode(GameType.FARBE, Suit.KREUZ);
                    case "5":
                        return new GameMode(GameType.GRAND, null);
                }
            }
        } else {
            return new GameMode(GameType.FARBE, Suit.KREUZ); // Bot-Standardwahl
        }
    }

    private int playTrick(int leaderIdx, GameMode mode) {
        Card[] playedCards = new Card[3];
        int current = leaderIdx;

        for (int i = 0; i < 3; i++) {
            Player p = players[current];
            Card card;
            if (p.isHuman) {
                card = humanPlayCard(p, playedCards[leaderIdx], mode);
            } else {
                card = botPlayCard(p, playedCards[leaderIdx], mode);
            }
            playedCards[current] = card;
            System.out.println(p.name + " spielt: " + card);
            current = (current + 1) % 3;
        }

        int winnerIdx = evaluateTrick(playedCards, leaderIdx, mode);
        Player winner = players[winnerIdx];
        System.out.println("-> " + winner.name + " gewinnt den Stich!");

        for (Card c : playedCards)
            winner.tricksWon.add(c);
        return winnerIdx;
    }

    private Card humanPlayCard(Player p, Card leadCard, GameMode mode) {
        printIndexedHand(p);
        while (true) {
            System.out.print("Welche Karte möchtest du spielen? (Nummer): ");
            int idx = readCardIndex(p.hand.size());
            Card candidate = p.hand.get(idx);
            if (isValidMove(p, candidate, leadCard, mode)) {
                return p.hand.remove(idx);
            }
            System.out.println("Ungültiger Zug! Du musst bedienen.");
        }
    }

    private Card botPlayCard(Player p, Card leadCard, GameMode mode) {
        for (int i = 0; i < p.hand.size(); i++) {
            Card c = p.hand.get(i);
            if (isValidMove(p, c, leadCard, mode)) {
                return p.hand.remove(i);
            }
        }
        return p.hand.remove(0);
    }

    private boolean isValidMove(Player p, Card candidate, Card leadCard, GameMode mode) {
        if (leadCard == null)
            return true; // Erste Karte im Stich ist immer frei

        boolean leadIsTrump = leadCard.isTrump(mode);
        boolean candidateIsTrump = candidate.isTrump(mode);

        if (leadIsTrump) {
            boolean hasTrump = p.hand.stream().anyMatch(c -> c.isTrump(mode));
            if (hasTrump)
                return candidateIsTrump;
            return true;
        } else {
            boolean hasSameSuit = p.hand.stream().anyMatch(c -> !c.isTrump(mode) && c.suit == leadCard.suit);
            if (hasSameSuit) {
                return !candidateIsTrump && candidate.suit == leadCard.suit;
            }
            return true;
        }
    }

    private int evaluateTrick(Card[] cards, int leaderIdx, GameMode mode) {
        int bestIdx = leaderIdx;
        Card bestCard = cards[leaderIdx];

        for (int i = 1; i < 3; i++) {
            int currentIdx = (leaderIdx + i) % 3;
            Card challenger = cards[currentIdx];

            if (isHigher(challenger, bestCard, mode)) {
                bestCard = challenger;
                bestIdx = currentIdx;
            }
        }
        return bestIdx;
    }

    private boolean isHigher(Card challenger, Card currentBest, GameMode mode) {
        boolean cTrump = challenger.isTrump(mode);
        boolean bTrump = currentBest.isTrump(mode);

        if (cTrump && !bTrump)
            return true;
        if (!cTrump && bTrump)
            return false;

        if (cTrump && bTrump) {
            if (challenger.isJack() && currentBest.isJack()) {
                return challenger.getJackPower() > currentBest.getJackPower();
            }
            if (challenger.isJack())
                return true;
            if (currentBest.isJack())
                return false;
            return challenger.rank.orderInSuit > currentBest.rank.orderInSuit;
        }

        // Beide Fehlkarten: sticht nur bei gleicher Farbe und höherem Rang
        if (challenger.suit == currentBest.suit) {
            return challenger.rank.orderInSuit > currentBest.rank.orderInSuit;
        }
        return false;
    }

    private void evaluateGame(Player declarer) {
        // Skat-Karten gehören am Ende zum Stichkonto des Alleinspielers
        declarer.tricksWon.addAll(skat);

        int declarerPoints = 0;
        for (Card c : declarer.tricksWon)
            declarerPoints += c.rank.points;
        int defendersPoints = 120 - declarerPoints;

        System.out.println("\n=================================");
        System.out.println("ERGEBNIS:");
        System.out.println(declarer.name + " (Alleinspieler): " + declarerPoints + " Augen");
        System.out.println("Gegenspieler: " + defendersPoints + " Augen");

        if (declarerPoints >= 61) {
            System.out.println("-> Alleinspieler " + declarer.name + " hat GEWONNEN (min. 61 Augen)!");
        } else {
            System.out.println("-> Alleinspieler " + declarer.name + " hat VERLOREN!");
        }
        System.out.println("=================================");
    }

    private void printHand(Player p) {
        System.out.println("\nDeine Handkarten:");
        for (Card c : p.hand)
            System.out.println("  • " + c);
    }

    private void printIndexedHand(Player p) {
        System.out.println("\nVerfügbare Handkarten:");
        for (int i = 0; i < p.hand.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + p.hand.get(i));
        }
    }

    private int readCardIndex(int max) {
        while (true) {
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= 1 && val <= max)
                    return val - 1;
            } catch (NumberFormatException ignored) {
            }
            System.out.print("Ungültige Eingabe. Wähle eine Zahl zwischen 1 und " + max + ": ");
        }
    }

    public static void main(String[] args) {
        new SkatSpiel().start();
    }
}