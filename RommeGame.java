import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RommeGame {

    public enum Suit {
        Hearts, Diamonds, Clubs, Spades
    }

    public enum Rank {
        Two(2, 2), Three(3, 3), Four(4, 4), Five(5, 5), Six(6, 6), Seven(7, 7), Eight(8, 8), Nine(9, 9), Ten(10, 10),
        Jack(11, 10), Queen(12, 10), King(13, 10), Ace(14, 11), Joker(15, 20);

        private final int order;
        private final int defaultPoints;

        Rank(int order, int defaultPoints) {
            this.order = order;
            this.defaultPoints = defaultPoints;
        }

        public int getOrder() {
            return order;
        }

        public int getDefaultPoints() {
            return defaultPoints;
        }
    }

    public static class Card {
        private final Suit suit;
        private final Rank rank;
        private final boolean isJoker;

        public Card(Suit suit, Rank rank) {
            this(suit, rank, false);
        }

        public Card(Suit suit, Rank rank, boolean isJoker) {
            this.suit = suit;
            this.rank = rank;
            this.isJoker = isJoker;
        }

        public static Card createJoker() {
            return new Card();
        }

        private Card() {
            this.suit = null;
            this.rank = Rank.Joker;
            this.isJoker = true;
        }

        public Suit getSuit() {
            return suit;
        }

        public Rank getRank() {
            return rank;
        }

        public boolean isJoker() {
            return isJoker;
        }

        @Override
        public String toString() {
            if (isJoker) {
                return "Joker";
            } else {
                return rank + "of" + suit;
            }
        }
    }

    public static class MeldValidator {
        /**
         * * Prüft, ob die Kartengruppe eine gültige Auslegung ist.
         */

        public static boolean isValidSet(List<Card> cards) {
            if (cards == null || cards.size() < 3 || cards.size() > 4) {
                return false;
            }
            Rank expectedRank = null;
            Set<Suit> suits = new HashSet<>();
            int jokerCount = 0;

            for (Card c : cards) {
                if (c.isJoker()) {
                    jokerCount++;
                } else {
                    if (expectedRank == null) {
                        expectedRank = c.getRank();
                    } else if (c.getRank() != expectedRank) {
                        return false;
                    }
                    if (!suits.add(c.getSuit())) {
                        return false;
                    }
                }
            }
            return jokerCount < cards.size();
        }

        public static boolean isValidSequence(List<Card> cards) {
            if (cards == null || cards.size() < 3) {
                return false;
            }
            Suit suit = null;
            int naturalCards = 0;

            for (Card c : cards) {
                if (!c.isJoker()) {
                    naturalCards++;
                    if (suit == null) {
                        suit = c.getSuit();
                    } else if (c.getSuit() != suit) {
                        return false;
                    }
                }
            }
            if (naturalCards == 0)
                return false;

            return checkConsecutive(cards, false) || checkConsecutive(cards, true);
        }

        private static boolean checkConsecutive(List<Card> cards, boolean aceAsOne) {
            int jokerAvailable = 0;
            List<Integer> orders = new ArrayList<>();
            for (Card c : cards) {
                if (c.isJoker()) {
                    jokerAvailable++;
                } else {
                    int val = (c.getRank() == Rank.Ace && aceAsOne) ? 1 : c.getRank().getOrder();
                    orders.add(val);
                }
            }
            Collections.sort(orders);

            for (int i = 0; i < orders.size() - 1; i++) {
                if (orders.get(i).equals(orders.get(i + 1))) {
                    return false;
                }
            }
            int neededJokers = 0;
            for (int i = 0; i < orders.size() - 1; i++) {
                int gap = orders.get(i + 1) - orders.get(i) - 1;
                neededJokers += gap;
            }

            if (neededJokers <= jokerAvailable) {
                int totalLength = orders.get(orders.size() - 1) - orders.get(0) + 1 + jokerAvailable - neededJokers;
                return totalLength <= 14;
            }
            return false;
        }

        public static int calculatePoints(List<Card> meld) {
            if (!isValidSet(meld) && !isValidSequence(meld)) {
                return 0;
            }
            int sum = 0;
            for (Card c : meld) {
                if (c.isJoker()) {
                    sum += 20;
                } else if (c.getRank() == Rank.Ace) {
                    sum += (isLowAceInMeld(meld, c)) ? 1 : 11;
                } else {
                    sum += c.getRank().getDefaultPoints();
                }
            }
            return sum;
        }

        private static boolean isLowAceInMeld(List<Card> meld, Card ace) {
            for (Card c : meld) {
                if (!c.isJoker() && c.getRank() == Rank.Two)
                    return true;
            }
            return false;
        }

        public static boolean canMakeInitialMeld(List<List<Card>> melds, int minPointsThreshold) {
            int total = 0;
            for (List<Card> meld : melds) {
                if (!isValidSet(meld) && !isValidSequence(meld)) {
                    return false;
                }
                total += calculatePoints(meld);
            }
            return total >= minPointsThreshold;
        }
    }

    public static void main(String[] args) {
        List<Card> satz = List.of(
                new Card(Suit.Hearts, Rank.Nine),
                new Card(Suit.Diamonds, Rank.Nine),
                Card.createJoker());
        System.out.println("Ist gültiger Satz: " + MeldValidator.isValidSet(satz));
        List<Card> sequenz = List.of(
                new Card(Suit.Clubs, Rank.Seven),
                new Card(Suit.Clubs, Rank.Eight),
                new Card(Suit.Clubs, Rank.Nine));
        System.out.println("Ist gültige Sequenz: " + MeldValidator.isValidSequence(sequenz));

        List<List<Card>> melds = List.of(satz, sequenz);
        boolean canOpen = MeldValidator.canMakeInitialMeld(melds, 30);

        int gesamtPunkte = melds.stream().mapToInt(MeldValidator::calculatePoints).sum();
        System.out.println("Gesamt Punkte: " + gesamtPunkte);
        System.out.println("Erstauslage erlaubt (>= 30): " + canOpen);
    }

    public class Player {
        private final String name;
        private final List<RommeGame.Card> hand = new ArrayList<>();
        private boolean hasOpened = false;

        public Player(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<RommeGame.Card> getHand() {
            return hand;
        }

        public boolean hasOpened() {
            return hasOpened;
        }

        public void setOpened(boolean opened) {
            this.hasOpened = opened;
        }

        public void addCard(RommeGame.Card card) {
            hand.add(card);
        }

        public boolean removeCard(RommeGame.Card card) {
            return hand.remove(card);
        }

        public boolean removeAll(Collection<RommeGame.Card> cards) {
            List<RommeGame.Card> copy = new ArrayList<>(hand);
            for (RommeGame.Card c : cards) {
                if (!copy.remove(c))
                    return false;
            }
            for (RommeGame.Card c : cards) {
                hand.remove(c);
            }
            return true;
        }

        @Override
        public String toString() {
            return name + " [Hand: " + hand.size() + " Karten, Ausgelegt: " + hasOpened + "]";
        }
    }

    public static record TableMeld(List<RommeGame.Card> cards) {
        public boolean canAddCard(RommeGame.Card card) {
            if (RommeGame.MeldValidator.isValidSet(cards)) {
                List<RommeGame.Card> test = new ArrayList<>(cards);
                test.add(card);
                return RommeGame.MeldValidator.isValidSet(test);
            }
            List<RommeGame.Card> testFront = new ArrayList(cards);
            testFront.add(0, card);
            if (RommeGame.MeldValidator.isValidSet(testFront)) {
                return true;
            }
            List<RommeGame.Card> testBack = new ArrayList(cards);
            testBack.add(card);
            return RommeGame.MeldValidator.isValidSequence(testBack);
        }
    }

    public boolean addCard(RommeGame.Card card) {
        if (!canAddCard(card))
            return false;

        if (RommeGame.MeldValidator.isValidSet(cards)) {
            cards.add(card);
            return true;
        }
        List<RommeGame.Card> testFront = new ArrayList<>(cards);
        testFront.add(0, card);
        if (RommeGame.MeldValidator.isValidSequence(testFront)) {
            cards.add(0, card);
            return true;
        }
        cards.add(card);
        return true;
    }

    public RommeGame.Card swapJokerWith(RommeGame.Card replacementCard) {
        for (int i = 0; i < cards.size(); i++) {
            RommeGame.Card joker = cards.get(i);
            cards.set(i, replacementCard);
            if (RommeGame.MeldValidator.isValidSet(cards) || RommeGame.MeldValidator.isValidSequence(cards)) {
                return joker;
            } else {
                cards.set(i, joker);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return cards.toString();
    }

    public enum TurnPhase {
        DRAW,
        MELD_OR_LAYOFF,
        DISCARD,
        ROUND_OVER
    }

    private final List<Player> players;
    private int currentPlayerIndex = 0;
    private TurnPhase currentPhase = TurnPhase.DRAW;

    private final Deque<RommeGame.Card> drawPile = new ArrayDeque<>();
    private final Deque<RommeGame.Card> discardPile = new ArrayDeque<>();
    private final List<TableMeld> tableMelds = new ArrayList<>();

    private final int openingThreshold;

    public RommeGame(List<Player> players, int openingThreshold) {
        this.players = players;
        this.openingThreshold = openingThreshold;
    }

    public Player getCurrentPlayer1() {
        return players.get(currentPlayerIndex);
    }

    public TurnPhase getCurrentPhase() {
        return currentPhase;
    }

    public List<TableMeld> getTableMelds() {
        return Collections.unmodifiableList(tableMelds);
    }

    public RommeGame.Card peekDiscardPile() {
        return discardPile.peek();
    }

    public void setupPiles(List<RommeGame.Card> deckCards) {
        drawPile.clear();
        discardPile.clear();
        drawPile.addAll(deckCards);

        if (!drawPile.isEmpty()) {
            discardPile.push(drawPile.pop());
        }
    }

    public RommeGame.Card drawFromStock() {
        validatePhase(TurnPhase.DRAW);
        if (drawPile.isEmpty()) {
            recycleDiscardPile();
        }
        RommeGame.Card drawn = drawPile.pop();
        getCurrentPlayer1().addCard(drawn);
        currentPhase = TurnPhase.MELD_OR_LAYOFF;
        return drawn;
    }

    public RommeGame.Card drawFromDiscard() {
        validatePhase(TurnPhase.DRAW);
        if (discardPile.isEmpty()) {
            throw new IllegalStateException("Ablagestapel ist leer!");
        }

        RommeGame.Card drawn = discardPile.pop();
        getCurrentPlayer1().addCard(drawn);
        currentPhase = TurnPhase.MELD_OR_LAYOFF;
        return drawn;
    }

    public boolean playNewMelds(List<List<RommeGame.Card>> newMelds) {
        validatePhase(TurnPhase.MELD_OR_LAYOFF);
        Player p = getCurrentPlayer1();

        if (!p.hasOpened()) {
            if (!RommeGame.MeldValidator.canMakeInitialMeld(newMelds, openingThreshold)) {
                return false;
            }
        } else {
            for (List<RommeGame.Card> meld : newMelds) {
                if (!RommeGame.MeldValidator.isValidSet(meld) && !RommeGame.MeldValidator.isValidSequence(meld)) {
                    return false;
                }
            }
        }

        List<RommeGame.Card> allCards = new ArrayList<>();
        newMelds.forEach(allCards::addAll);
        if (!p.removeAll(allCards)) {
            return false;
        }

        for (List<RommeGame.Card> meld : newMelds) {
            tableMelds.add(new TableMeld(meld));
        }

        p.setOpened(true);
        return true;
    }

    public boolean layOffCard(int tableMeldIndex, RommeGame.Card card) {
        validatePhase(TurnPhase.MELD_OR_LAYOFF);
        Player p = getCurrentPlayer1();

        if (!p.hasOpened()) {
            throw new IllegalStateException("Anlegen ist erst nach erfolgter Erstauslage erlaubt!");
        }
        if (tableMeldIndex < 0 || tableMeldIndex >= tableMelds.size()) {
            return false;
        }
        if (!p.getHand().contains(card)) {
            return false;
        }
        TableMeld targetMeld = tableMelds.get(tableMeldIndex);
        if (targetMeld.addCard(card)) {
            p.removeCard(card);
            return true;
        }
        return false;
    }

    public boolean swapJoker(int tableMeldIndex, RommeGame.Card handCard) {
        validatePhase(TurnPhase.MELD_OR_LAYOFF);
        Player p = getCurrentPlayer1();
        if (!p.hasOpened()) {
            throw new IllegalStateException("Joker-Tausch ist erst nach erfolgter Erstauslegung möglich!");
        }
        if (!p.getHand().contains(handCard)) {
            return false;
        }
        TableMeld targetMeld = tableMelds.get(tableMeldIndex);
        RommeGame.Card freedJoker = targetMeld.swapJokerWith(handCard);
        if (freedJoker != null) {
            p.removeCard(handCard);
            p.addCard(freedJoker);
            return true;
        }
        return false;
    }

    public void discard(RommeGame.Card card) {
        if (currentPhase == TurnPhase.MELD_OR_LAYOFF) {
            currentPhase = TurnPhase.DISCARD;
        }
        validatePhase(TurnPhase.DISCARD);

        Player p = getCurrentPlayer1();
        if (!p.removeCard(card)) {
            throw new IllegalArgumentException("Karte befindet sich nicht auf der Hand!");
        }

        discardPile.push(card);

        if (p.getHand().isEmpty()) {
            currentPhase = TurnPhase.ROUND_OVER;
            System.out.println(">>> " + p.getName() + " hat die Runde beendet und gewonnen! <<<");
            return;
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        currentPhase = TurnPhase.DRAW;
    }

    private void recycleDiscardPile() {
        if (discardPile.size() <= 1) {
            throw new IllegalStateException("Keine Karten mehr zum Nachziehen vorhanden!");
        }
        RommeGame.Card top = discardPile.pop();
        List<RommeGame.Card> list = new ArrayList<>(discardPile);
        Collections.shuffle(list);
        discardPile.clear();
        discardPile.addAll(list);
        discardPile.push(top);
    }

    private void validatePhase(TurnPhase expected) {
        if (currentPhase != expected) {
            throw new IllegalStateException(
                    "Ungültige Aktion in Phase: " + currentPhase + " (Erwartet: " + expected + ")");
        }
    }
}