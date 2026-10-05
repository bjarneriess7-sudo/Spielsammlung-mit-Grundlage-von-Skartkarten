package CardGamesProject.src;

public enum PokerHandRank {
    HIGH_CARD,
    ONE_PAIR,
    TWO_PAIR,
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH,
    ROYAL_FLUSH
}
package CardGamesProject.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HandEvaluation implements Comparable<HandEvaluation> {
    private final PokerHandRank handRank;
    private final List<Integer> tieBreakers;

    public HandEvaluation(PokerHandRank handRank, List<Integer> tieBreakers) {
        if (handRank == null || tieBreakers == null) {
            throw new IllegalArgumentException("HandRank und TieBreakers duerfen nicht null sein.");
        }
        this.handRank = handRank;
        this.tieBreakers = new ArrayList<>(tieBreakers);
    }

    public PokerHandRank getHandRank() {
        return handRank;
    }

    public List<Integer> getTieBreakers() {
        return Collections.unmodifiableList(tieBreakers);
    }

    @Override
    public int compareTo(HandEvaluation other) {
        int rankComparison = Integer.compare(this.handRank.ordinal(), other.handRank.ordinal());
        if (rankComparison != 0) {
            return rankComparison;
        }
        int minSize = Math.min(this.tieBreakers.size(), other.tieBreakers.size());
        for (int i = 0; i < minSize; i++) {
            int comp = Integer.compare(this.tieBreakers.get(i), other.tieBreakers.get(i));
            if (comp != 0) {
                return comp;
            }
        }
        return Integer.compare(this.tieBreakers.size(), other.tieBreakers.size());
    }
}
package CardGamesProject.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PokerHandEvaluator {
    private PokerHandEvaluator() {
    }

    public static HandEvaluation evaluate(List<Card> allCards) {
        if (allCards == null || allCards.size() < 5) {
            throw new IllegalArgumentException("Zur Handbewertung werden mindestens 5 Karten benoetigt.");
        }

        Map<Suit, List<Card>> suitMap = new HashMap<>();
        Map<Integer, Integer> rankCounts = new HashMap<>();

        for (Card card : allCards) {
            suitMap.computeIfAbsent(card.getSuit(), k -> new ArrayList<>()).add(card);
            int value = getCardValue(card.getRank());
            rankCounts.put(value, rankCounts.getOrDefault(value, 0) + 1);
        }

        for (Map.Entry<Suit, List<Card>> entry : suitMap.entrySet()) {
            if (entry.getValue().size() >= 5) {
                List<Integer> flushRanks = new ArrayList<>();
                for (Card c : entry.getValue()) {
                    flushRanks.add(getCardValue(c.getRank()));
                }
                Collections.sort(flushRanks, Collections.reverseOrder());

                List<Integer> straightFlushTies = getStraightHighRanks(flushRanks);
                if (!straightFlushTies.isEmpty()) {
                    if (straightFlushTies.get(0) == 14) {
                        return new HandEvaluation(PokerHandRank.ROYAL_FLUSH, straightFlushTies);
                    }
                    return new HandEvaluation(PokerHandRank.STRAIGHT_FLUSH, straightFlushTies);
                }
                return new HandEvaluation(PokerHandRank.FLUSH, flushRanks.subList(0, 5));
            }
        }

        List<Integer> distinctSortedRanks = new ArrayList<>(rankCounts.keySet());
        Collections.sort(distinctSortedRanks, Collections.reverseOrder());

        List<Integer> fourOfAKind = new ArrayList<>();
        List<Integer> threeOfAKind = new ArrayList<>();
        List<Integer> pairs = new ArrayList<>();
        List<Integer> singles = new ArrayList<>();

        for (int rankVal : distinctSortedRanks) {
            int count = rankCounts.get(rankVal);
            if (count == 4) {
                fourOfAKind.add(rankVal);
            } else if (count == 3) {
                threeOfAKind.add(rankVal);
            } else if (count == 2) {
                pairs.add(rankVal);
            } else {
                singles.add(rankVal);
            }
        }

        if (!fourOfAKind.isEmpty()) {
            int quad = fourOfAKind.get(0);
            int kicker = getHighestKickerExcept(distinctSortedRanks, quad);
            List<Integer> ties = new ArrayList<>();
            ties.add(quad);
            ties.add(kicker);
            return new HandEvaluation(PokerHandRank.FOUR_OF_A_KIND, ties);
        }

        if (!threeOfAKind.isEmpty() && (!pairs.isEmpty() || threeOfAKind.size() > 1)) {
            int trio = threeOfAKind.get(0);
            int pair = (threeOfAKind.size() > 1) ? threeOfAKind.get(1) : pairs.get(0);
            List<Integer> ties = new ArrayList<>();
            ties.add(trio);
            ties.add(pair);
            return new HandEvaluation(PokerHandRank.FULL_HOUSE, ties);
        }

        List<Integer> straightRanks = getStraightHighRanks(distinctSortedRanks);
        if (!straightRanks.isEmpty()) {
            return new HandEvaluation(PokerHandRank.STRAIGHT, straightRanks);
        }

        if (!threeOfAKind.isEmpty()) {
            int trio = threeOfAKind.get(0);
            List<Integer> kickers = getTopKickers(distinctSortedRanks, Collections.singletonList(trio), 2);
            List<Integer> ties = new ArrayList<>();
            ties.add(trio);
            ties.addAll(kickers);
            return new HandEvaluation(PokerHandRank.THREE_OF_A_KIND, ties);
        }

        if (pairs.size() >= 2) {
            int p1 = pairs.get(0);
            int p2 = pairs.get(1);
            List<Integer> excluded = new ArrayList<>();
            excluded.add(p1);
            excluded.add(p2);
            List<Integer> kicker = getTopKickers(distinctSortedRanks, excluded, 1);
            List<Integer> ties = new ArrayList<>();
            ties.add(p1);
            ties.add(p2);
            ties.addAll(kicker);
            return new HandEvaluation(PokerHandRank.TWO_PAIR, ties);
        }

        if (pairs.size() == 1) {
            int p = pairs.get(0);
            List<Integer> kickers = getTopKickers(distinctSortedRanks, Collections.singletonList(p), 3);
            List<Integer> ties = new ArrayList<>();
            ties.add(p);
            ties.addAll(kickers);
            return new HandEvaluation(PokerHandRank.ONE_PAIR, ties);
        }

        return new HandEvaluation(PokerHandRank.HIGH_CARD, distinctSortedRanks.subList(0, 5));
    }

    private static List<Integer> getStraightHighRanks(List<Integer> sortedRanks) {
        List<Integer> unique = new ArrayList<>();
        for (int r : sortedRanks) {
            if (!unique.contains(r)) {
                unique.add(r);
            }
        }
        if (unique.contains(14)) {
            unique.add(1);
        }
        for (int i = 0; i <= unique.size() - 5; i++) {
            if (unique.get(i) - 1 == unique.get(i + 1)
                    && unique.get(i + 1) - 1 == unique.get(i + 2)
                    && unique.get(i + 2) - 1 == unique.get(i + 3)
                    && unique.get(i + 3) - 1 == unique.get(i + 4)) {
                return Collections.singletonList(unique.get(i));
            }
        }
        return Collections.emptyList();
    }

    private static int getHighestKickerExcept(List<Integer> sorted, int excluded) {
        for (int r : sorted) {
            if (r != excluded) {
                return r;
            }
        }
        return 0;
    }

    private static List<Integer> getTopKickers(List<Integer> sorted, List<Integer> excluded, int count) {
        List<Integer> result = new ArrayList<>();
        for (int r : sorted) {
            if (!excluded.contains(r)) {
                result.add(r);
                if (result.size() == count) {
                    break;
                }
            }
        }
        return result;
    }

    public static int getCardValue(Rank rank) {
        switch (rank) {
            case TWO: return 2;
            case THREE: return 3;
            case FOUR: return 4;
            case FIVE: return 5;
            case SIX: return 6;
            case SEVEN: return 7;
            case EIGHT: return 8;
            case NINE: return 9;
            case TEN: return 10;
            case JACK: return 11;
            case QUEEN: return 12;
            case KING: return 13;
            case ACE: return 14;
            default: throw new IllegalArgumentException("Unbekannter Kartenrang");
        }
    }
}
package CardGamesProject.src;

public class PokerPlayer implements Player {
    private final String id;
    private final String name;
    private final Hand hand;
    private double chips;
    private double currentBetInRound;
    private boolean folded;

    public PokerPlayer(String id, String name, double initialChips) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("ID und Name duerfen nicht leer sein.");
        }
        if (initialChips < 0) {
            throw new IllegalArgumentException("Initiales Guthaben darf nicht negativ sein.");
        }
        this.id = id;
        this.name = name;
        this.chips = initialChips;
        this.hand = new Hand();
        this.currentBetInRound = 0.0;
        this.folded = false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public Hand getHand() {
        return hand;
    }

    public double getChips() {
        return chips;
    }

    public double getCurrentBetInRound() {
        return currentBetInRound;
    }

    public void addChips(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Betrag darf nicht negativ sein.");
        }
        this.chips += amount;
    }

    public double placeBet(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Einsatz darf nicht negativ sein.");
        }
        double actualBet = Math.min(amount, this.chips);
        this.chips -= actualBet;
        this.currentBetInRound += actualBet;
        return actualBet;
    }

    public void resetBetForNextRound() {
        this.currentBetInRound = 0.0;
    }

    public boolean isFolded() {
        return folded;
    }

    public void fold() {
        this.folded = true;
    }

    public void resetForNewHand() {
        this.hand.clear();
        this.currentBetInRound = 0.0;
        this.folded = false;
    }
}
package CardGamesProject.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PokerGame implements PlayableGame {
    private final List<PokerPlayer> players;
    private final List<Card> communityCards;
    private final Deck deck;
    private final double smallBlind;
    private final double bigBlind;
    private double pot;
    private double currentHighestBet;
    private int dealerButtonIndex;
    private boolean active;

    public PokerGame(List<PokerPlayer> players, double smallBlind, double bigBlind) {
        if (players == null || players.size() < 2) {
            throw new IllegalArgumentException("Poker erfordert mindestens zwei Spieler.");
        }
        if (smallBlind <= 0 || bigBlind <= smallBlind) {
            throw new IllegalArgumentException("Ungueltige Blind-Konfiguration.");
        }
        this.players = new ArrayList<>(players);
        this.communityCards = new ArrayList<>();
        this.deck = new Deck();
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.dealerButtonIndex = 0;
        this.active = false;
    }

    @Override
    public void start() {
        this.deck.shuffle();
        this.communityCards.clear();
        this.pot = 0.0;
        this.currentHighestBet = 0.0;
        this.active = true;

        for (PokerPlayer p : players) {
            p.resetForNewHand();
        }

        try {
            dealHoleCards();
            postBlinds();
        } catch (EmptyDeckException e) {
            throw new IllegalStateException("Unerwartetes leeres Deck beim Spielstart.", e);
        }
    }

    private void dealHoleCards() throws EmptyDeckException {
        for (int i = 0; i < 2; i++) {
            for (PokerPlayer p : players) {
                p.getHand().addCard(deck.drawCard());
            }
        }
    }

    private void postBlinds() {
        int sbIndex = (dealerButtonIndex + 1) % players.size();
        int bbIndex = (dealerButtonIndex + 2) % players.size();

        pot += players.get(sbIndex).placeBet(smallBlind);
        pot += players.get(bbIndex).placeBet(bigBlind);
        currentHighestBet = bigBlind;
    }

    public void dealFlop() throws EmptyDeckException {
        deck.drawCard(); // Burn Card
        for (int i = 0; i < 3; i++) {
            communityCards.add(deck.drawCard());
        }
        resetBetsForStreet();
    }

    public void dealTurn() throws EmptyDeckException {
        deck.drawCard(); // Burn Card
        communityCards.add(deck.drawCard());
        resetBetsForStreet();
    }

    public void dealRiver() throws EmptyDeckException {
        deck.drawCard(); // Burn Card
        communityCards.add(deck.drawCard());
        resetBetsForStreet();
    }

    private void resetBetsForStreet() {
        this.currentHighestBet = 0.0;
        for (PokerPlayer p : players) {
            p.resetBetForNextRound();
        }
    }

    public void playerCallOrCheck(PokerPlayer player) {
        if (!active || player.isFolded()) {
            throw new IllegalStateException("Spieler kann diese Aktion nicht ausfuehren.");
        }
        double toCall = currentHighestBet - player.getCurrentBetInRound();
        if (toCall > 0) {
            pot += player.placeBet(toCall);
        }
    }

    public void playerRaise(PokerPlayer player, double raiseAmount) {
        if (!active || player.isFolded()) {
            throw new IllegalStateException("Spieler kann diese Aktion nicht ausfuehren.");
        }
        double targetBet = currentHighestBet + raiseAmount;
        double additionalNeeded = targetBet - player.getCurrentBetInRound();
        pot += player.placeBet(additionalNeeded);
        currentHighestBet = player.getCurrentBetInRound();
    }

    public void playerFold(PokerPlayer player) {
        if (!active || player.isFolded()) {
            throw new IllegalStateException("Spieler kann nicht folden.");
        }
        player.fold();
        checkSinglePlayerRemaining();
    }

    private void checkSinglePlayerRemaining() {
        List<PokerPlayer> activeRemaining = getActiveInHandPlayers();
        if (activeRemaining.size() == 1) {
            PokerPlayer winner = activeRemaining.get(0);
            winner.addChips(pot);
            pot = 0.0;
            endGame();
        }
    }

    public List<PokerPlayer> showdown() {
        if (!active) {
            throw new IllegalStateException("Kein aktives Spiel fuer den Showdown.");
        }
        List<PokerPlayer> activeInHand = getActiveInHandPlayers();
        if (activeInHand.isEmpty()) {
            endGame();
            return Collections.emptyList();
        }

        List<PokerPlayer> winners = new ArrayList<>();
        HandEvaluation bestEval = null;

        for (PokerPlayer p : activeInHand) {
            List<Card> fullSet = new ArrayList<>(p.getHand().getCards());
            fullSet.addAll(communityCards);
            HandEvaluation eval = PokerHandEvaluator.evaluate(fullSet);

            if (bestEval == null || eval.compareTo(bestEval) > 0) {
                bestEval = eval;
                winners.clear();
                winners.add(p);
            } else if (eval.compareTo(bestEval) == 0) {
                winners.add(p);
            }
        }

        double splitPot = pot / winners.size();
        for (PokerPlayer w : winners) {
            w.addChips(splitPot);
        }
        pot = 0.0;
        dealerButtonIndex = (dealerButtonIndex + 1) % players.size();
        endGame();
        return winners;
    }

    public List<PokerPlayer> getActiveInHandPlayers() {
        List<PokerPlayer> list = new ArrayList<>();
        for (PokerPlayer p : players) {
            if (!p.isFolded()) {
                list.add(p);
            }
        }
        return list;
    }

    public List<Card> getCommunityCards() {
        return Collections.unmodifiableList(communityCards);
    }

    public double getPot() {
        return pot;
    }

    @Override
    public boolean isGameOver() {
        return !active;
    }

    @Override
    public void endGame() {
        this.active = false;
    }
}
package CardGamesProject.src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PokerTestRunner {
    public static void main(String[] args) {
        testStraightAndFlushEvaluation();
        testFullHouseAndPairEvaluation();
        testGameFlowAndShowdown();
        System.out.println("Alle Poker-Tests erfolgreich abgeschlossen.");
    }

    private static void testStraightAndFlushEvaluation() {
        List<Card> flushCards = Arrays.asList(
            new Card(Suit.HEARTS, Rank.TWO),
            new Card(Suit.HEARTS, Rank.FIVE),
            new Card(Suit.HEARTS, Rank.SEVEN),
            new Card(Suit.HEARTS, Rank.JACK),
            new Card(Suit.HEARTS, Rank.ACE),
            new Card(Suit.SPADES, Rank.KING),
            new Card(Suit.CLUBS, Rank.THREE)
        );
        HandEvaluation flushEval = PokerHandEvaluator.evaluate(flushCards);
        if (flushEval.getHandRank() != PokerHandRank.FLUSH) {
            throw new AssertionError("Flush wurde nicht erkannt.");
        }

        List<Card> straightCards = Arrays.asList(
            new Card(Suit.HEARTS, Rank.FIVE),
            new Card(Suit.SPADES, Rank.SIX),
            new Card(Suit.CLUBS, Rank.SEVEN),
            new Card(Suit.DIAMONDS, Rank.EIGHT),
            new Card(Suit.HEARTS, Rank.NINE),
            new Card(Suit.CLUBS, Rank.KING),
            new Card(Suit.DIAMONDS, Rank.TWO)
        );
        HandEvaluation straightEval = PokerHandEvaluator.evaluate(straightCards);
        if (straightEval.getHandRank() != PokerHandRank.STRAIGHT) {
            throw new AssertionError("Straight wurde nicht erkannt.");
        }
    }

    private static void testFullHouseAndPairEvaluation() {
        List<Card> fullHouseCards = Arrays.asList(
            new Card(Suit.HEARTS, Rank.TEN),
            new Card(Suit.SPADES, Rank.TEN),
            new Card(Suit.CLUBS, Rank.TEN),
            new Card(Suit.DIAMONDS, Rank.FOUR),
            new Card(Suit.HEARTS, Rank.FOUR),
            new Card(Suit.CLUBS, Rank.TWO),
            new Card(Suit.SPADES, Rank.NINE)
        );
        HandEvaluation fhEval = PokerHandEvaluator.evaluate(fullHouseCards);
        if (fhEval.getHandRank() != PokerHandRank.FULL_HOUSE) {
            throw new AssertionError("Full House wurde nicht erkannt.");
        }

        List<Card> pairCards = Arrays.asList(
            new Card(Suit.HEARTS, Rank.ACE),
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.CLUBS, Rank.KING),
            new Card(Suit.DIAMONDS, Rank.QUEEN),
            new Card(Suit.HEARTS, Rank.JACK),
            new Card(Suit.CLUBS, Rank.TWO),
            new Card(Suit.SPADES, Rank.THREE)
        );
        HandEvaluation pairEval = PokerHandEvaluator.evaluate(pairCards);
        if (pairEval.getHandRank() != PokerHandRank.ONE_PAIR) {
            throw new AssertionError("Pair wurde nicht erkannt.");
        }
    }

    private static void testGameFlowAndShowdown() {
        PokerPlayer p1 = new PokerPlayer("p1", "Alice", 1000.0);
        PokerPlayer p2 = new PokerPlayer("p2", "Bob", 1000.0);
        PokerGame game = new PokerGame(Arrays.asList(p1, p2), 10.0, 20.0);

        game.start();

        if (p1.getHand().getCardCount() != 2 || p2.getHand().getCardCount() != 2) {
            throw new AssertionError("Spieler muessen je 2 Startkarten erhalten.");
        }
        if (game.getPot() != 30.0) {
            throw new AssertionError("Pot nach Blinds fehlerhaft. Erwartet: 30.0, Ist: " + game.getPot());
        }

        try {
            game.dealFlop();
            game.dealTurn();
            game.dealRiver();
        } catch (EmptyDeckException e) {
            throw new AssertionError("Unerwarteter Deckfehler im Spielverlauf.", e);
        }

        if (game.getCommunityCards().size() != 5) {
            throw new AssertionError("Gemeinschaftskarten muessen 5 Karten betragen.");
        }

        List<PokerPlayer> winners = game.showdown();
        if (winners.isEmpty() || !game.isGameOver()) {
            throw new AssertionError("Showdown-Ermittlung fehlerhaft.");
        }
    }
}