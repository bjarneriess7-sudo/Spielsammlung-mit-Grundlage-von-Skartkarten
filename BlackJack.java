package CardGamesProject.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackjackPlayer implements Player {
    private final String id;
    private final String name;
    private final List<Hand> hands;
    private final List<Double> currentBets;
    private boolean isSplit;

    public BlackjackPlayer(String id, String name) {
        this.id = id;
        this.name = name;
        this.hands = new ArrayList<>();
        this.currentBets = new ArrayList<>();
        resetHands();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void resetHands() {
        hands.clear();
        currentBets.clear();
        hands.add(new Hand());
        currentBets.add(0.0);
        isSplit = false;
    }

    public Hand getHand(int handIndex) {
        return hands.get(handIndex);
    }

    public List<Hand> getHands() {
        return Collections.unmodifiableList(hands);
    }

    public double getBet(int handIndex) {
        return currentBets.get(handIndex);
    }

    public void setBet(int handIndex, double bet) {
        currentBets.set(handIndex, bet);
    }

    public boolean canSplit() {
        if (hands.size() != 1 || isSplit) {
            return false;
        }
        Hand h = hands.get(0);
        if (h.getCards().size() != 2) {
            return false;
        }
        return calculateCardValue(h.getCards().get(0)) == calculateCardValue(h.getCards().get(1));
    }

    public void splitHand() {
        if (!canSplit()) {
            throw new IllegalStateException("Split ist mit den aktuellen Karten nicht zulaessig.");
        }
        Hand firstHand = hands.get(0);
        Card secondCard = firstHand.removeCard(1);
        Hand secondHand = new Hand();
        secondHand.addCard(secondCard);
        hands.add(secondHand);
        currentBets.add(currentBets.get(0));
        isSplit = true;
    }

    public static int calculateHandValue(Hand hand) {
        int total = 0;
        int aceCount = 0;
        for (Card card : hand.getCards()) {
            int val = calculateCardValue(card);
            if (card.getRank() == Rank.ACE) {
                aceCount++;
            }
            total += val;
        }
        while (total > 21 && aceCount > 0) {
            total -= 10;
            aceCount--;
        }
        return total;
    }

    public static int calculateCardValue(Card card) {
        Rank r = card.getRank();
        switch (r) {
            case TWO: return 2;
            case THREE: return 3;
            case FOUR: return 4;
            case FIVE: return 5;
            case SIX: return 6;
            case SEVEN: return 7;
            case EIGHT: return 8;
            case NINE: return 9;
            case TEN:
            case JACK:
            case QUEEN:
            case KING: return 10;
            case ACE: return 11;
            default: return 0;
        }
    }

    public boolean isBusted(int handIndex) {
        return calculateHandValue(hands.get(handIndex)) > 21;
    }

    public boolean isNaturalBlackjack(int handIndex) {
        return !isSplit && hands.get(handIndex).getCards().size() == 2 && calculateHandValue(hands.get(handIndex)) == 21;
    }
}
package CardGamesProject.src;

public class BlackjackDealerAi {
    public static final int DEALER_STAND_THRESHOLD = 17;

    public boolean shouldDealerHit(Hand dealerHand) {
        int score = BlackjackPlayer.calculateHandValue(dealerHand);
        return score < DEALER_STAND_THRESHOLD;
    }

    public void playDealerTurn(Deck deck, Hand dealerHand) throws EmptyDeckException {
        while (shouldDealerHit(dealerHand)) {
            dealerHand.addCard(deck.drawCard());
        }
    }

    public double calculateBustProbability(Hand dealerHand) {
        int score = BlackjackPlayer.calculateHandValue(dealerHand);
        if (score >= 21) {
            return 1.0;
        }
        if (score <= 11) {
            return 0.0;
        }
        int diff = 21 - score;
        int bustCardsCount = 0;
        for (int rankVal = 1; rankVal <= 10; rankVal++) {
            if (rankVal > diff) {
                bustCardsCount += (rankVal == 10) ? 4 : 1;
            }
        }
        return bustCardsCount / 13.0;
    }
}
package CardGamesProject.src;

import java.util.HashMap;
import java.util.Map;

public class BettingSystem {
    private final double minBet;
    private final double maxBet;
    private final Map<String, Double> accounts;

    public BettingSystem(double minBet, double maxBet) {
        if (minBet <= 0 || maxBet < minBet) {
            throw new IllegalArgumentException("Ungueltige Einsatzlimits konfiguriert.");
        }
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.accounts = new HashMap<>();
    }

    public void registerAccount(String playerId, double initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Startguthaben darf nicht negativ sein.");
        }
        accounts.put(playerId, initialBalance);
    }

    public double getBalance(String playerId) {
        return accounts.getOrDefault(playerId, 0.0);
    }

    public void placeBet(String playerId, double amount) {
        validateBetAmount(amount);
        double current = getBalance(playerId);
        if (current < amount) {
            throw new IllegalStateException("Nicht genuegend Guthaben verfuegbar.");
        }
        accounts.put(playerId, current - amount);
    }

    public void payout(String playerId, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Auszahlungsbetrag darf nicht negativ sein.");
        }
        accounts.put(playerId, getBalance(playerId) + amount);
    }

    public void validateBetAmount(double amount) {
        if (amount < minBet || amount > maxBet) {
            throw new IllegalArgumentException("Einsatz liegt ausserhalb der Limits: " + minBet + " - " + maxBet);
        }
    }

    public double getMinBet() {
        return minBet;
    }

    public double getMaxBet() {
        return maxBet;
    }
}
package CardGamesProject.src;

import java.util.ArrayList;
import java.util.List;

public class BlackjackGame implements PlayableGame {
    private final Deck deck;
    private final List<BlackjackPlayer> players;
    private final Hand dealerHand;
    private final BlackjackDealerAi dealerAi;
    private final BettingSystem bettingSystem;
    private boolean running;

    public BlackjackGame(Deck deck, BettingSystem bettingSystem) {
        this.deck = deck;
        this.bettingSystem = bettingSystem;
        this.players = new ArrayList<>();
        this.dealerHand = new Hand();
        this.dealerAi = new BlackjackDealerAi();
        this.running = false;
    }

    public void addPlayer(BlackjackPlayer player) {
        players.add(player);
    }

    @Override
    public void start() {
        if (players.isEmpty()) {
            throw new IllegalStateException("Keine Spieler am Tisch.");
        }
        deck.shuffle();
        dealerHand.clear();
        running = true;
    }

    public void dealInitialCards() throws EmptyDeckException {
        for (int i = 0; i < 2; i++) {
            for (BlackjackPlayer p : players) {
                p.getHand(0).addCard(deck.drawCard());
            }
            dealerHand.addCard(deck.drawCard());
        }
    }

    public void playerHit(BlackjackPlayer player, int handIndex) throws EmptyDeckException {
        if (!running) {
            throw new IllegalStateException("Spiel laeuft nicht.");
        }
        player.getHand(handIndex).addCard(deck.drawCard());
    }

    public void playerDoubleDown(BlackjackPlayer player, int handIndex) throws EmptyDeckException {
        double currentBet = player.getBet(handIndex);
        bettingSystem.placeBet(player.getId(), currentBet);
        player.setBet(handIndex, currentBet * 2);
        player.getHand(handIndex).addCard(deck.drawCard());
    }

    public void playerSplit(BlackjackPlayer player) throws EmptyDeckException {
        double initialBet = player.getBet(0);
        bettingSystem.placeBet(player.getId(), initialBet);
        player.splitHand();
        player.getHand(0).addCard(deck.drawCard());
        player.getHand(1).addCard(deck.drawCard());
    }

    public void resolveRound() throws EmptyDeckException {
        dealerAi.playDealerTurn(deck, dealerHand);
        int dealerScore = BlackjackPlayer.calculateHandValue(dealerHand);
        boolean dealerBust = dealerScore > 21;

        for (BlackjackPlayer player : players) {
            for (int i = 0; i < player.getHands().size(); i++) {
                Hand h = player.getHand(i);
                double bet = player.getBet(i);
                int playerScore = BlackjackPlayer.calculateHandValue(h);

                if (playerScore > 21) {
                    continue;
                }
                if (player.isNaturalBlackjack(i)) {
                    if (dealerHand.getCards().size() == 2 && dealerScore == 21) {
                        bettingSystem.payout(player.getId(), bet);
                    } else {
                        bettingSystem.payout(player.getId(), bet + (bet * 1.5));
                    }
                } else if (dealerBust || playerScore > dealerScore) {
                    bettingSystem.payout(player.getId(), bet * 2);
                } else if (playerScore == dealerScore) {
                    bettingSystem.payout(player.getId(), bet);
                }
            }
        }
        endGame();
    }

    public Hand getDealerHand() {
        return dealerHand;
    }

    @Override
    public boolean isGameOver() {
        return !running;
    }

    @Override
    public void endGame() {
        running = false;
    }
}
package CardGamesProject.src;

public class BlackjackTestRunner {
    public static void main(String[] args) {
        testAceEvaluationLogic();
        testSplitAndDoubleDown();
        testDealerAiLogic();
        System.out.println("Alle Blackjack-Tests erfolgreich durchgelaufen.");
    }

    private static void testAceEvaluationLogic() {
        Hand aceHand = new Hand();
        aceHand.addCard(new Card(Suit.HEARTS, Rank.ACE));
        aceHand.addCard(new Card(Suit.CLUBS, Rank.EIGHT));
        if (BlackjackPlayer.calculateHandValue(aceHand) != 19) {
            throw new AssertionError("Soft 19 Berechnung fehlerhaft");
        }

        aceHand.addCard(new Card(Suit.DIAMONDS, Rank.FIVE));
        if (BlackjackPlayer.calculateHandValue(aceHand) != 14) {
            throw new AssertionError("Ass-Reduktion von 11 auf 1 fehlerhaft");
        }

        aceHand.addCard(new Card(Suit.SPADES, Rank.ACE));
        if (BlackjackPlayer.calculateHandValue(aceHand) != 15) {
            throw new AssertionError("Zweites Ass Berechnung fehlerhaft");
        }
    }

    private static void testSplitAndDoubleDown() {
        BettingSystem bs = new BettingSystem(10.0, 500.0);
        bs.registerAccount("p1", 1000.0);
        BlackjackPlayer player = new BlackjackPlayer("p1", "Alice");
        player.setBet(0, 50.0);
        bs.placeBet("p1", 50.0);

        player.getHand(0).addCard(new Card(Suit.HEARTS, Rank.EIGHT));
        player.getHand(0).addCard(new Card(Suit.SPADES, Rank.EIGHT));

        if (!player.canSplit()) {
            throw new AssertionError("Split-Bedingung fuer ein 8er-Paar nicht erkannt");
        }

        bs.placeBet("p1", 50.0);
        player.splitHand();

        if (player.getHands().size() != 2) {
            throw new AssertionError("Hand wurde nicht erfolgreich in zwei Haende aufgeteilt");
        }
        if (player.getBet(0) != 50.0 || player.getBet(1) != 50.0) {
            throw new AssertionError("Wetteinsaetze fuer Split fehlerhaft");
        }

        double doubleDownBet = player.getBet(0);
        bs.placeBet("p1", doubleDownBet);
        player.setBet(0, doubleDownBet * 2);
        if (player.getBet(0) != 100.0) {
            throw new AssertionError("Double Down Einsatzverdopplung fehlgeschlagen");
        }
    }

    private static void testDealerAiLogic() {
        BlackjackDealerAi ai = new BlackjackDealerAi();
        Hand dealerHand = new Hand();
        dealerHand.addCard(new Card(Suit.HEARTS, Rank.TEN));
        dealerHand.addCard(new Card(Suit.CLUBS, Rank.SIX));

        if (!ai.shouldDealerHit(dealerHand)) {
            throw new AssertionError("Dealer muss bei 16 Punkten ziehen");
        }

        dealerHand.addCard(new Card(Suit.DIAMONDS, Rank.ACE)); // 17 Punkte
        if (ai.shouldDealerHit(dealerHand)) {
            throw new AssertionError("Dealer muss bei 17 Punkten stehen bleiben");
        }

        double bustProbAt16 = ai.calculateBustProbability(dealerHand);
        if (bustProbAt16 <= 0.0 || bustProbAt16 > 1.0) {
            throw new AssertionError("Bust-Wahrscheinlichkeitsberechnung ausserhalb des Wertebereichs");
        }
    }
}