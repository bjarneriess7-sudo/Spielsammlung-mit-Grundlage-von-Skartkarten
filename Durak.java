package CardGamesProject.src;

public class DurakPlayer implements Player {
    private final String id;
    private final String name;
    private final Hand hand;
    private boolean isAttacker;
    private boolean isDefender;

    public DurakPlayer(String id, String name) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("ID und Name duerfen nicht leer sein.");
        }
        this.id = id;
        this.name = name;
        this.hand = new Hand();
        this.isAttacker = false;
        this.isDefender = false;
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

    public boolean isAttacker() {
        return isAttacker;
    }

    public void setAttacker(boolean attacker) {
        this.isAttacker = attacker;
    }

    public boolean isDefender() {
        return isDefender;
    }

    public void setDefender(boolean defender) {
        this.isDefender = defender;
    }

    public boolean hasCards() {
        return hand.getCardCount() > 0;
    }

    public Card playCard(int index) {
        if (index < 0 || index >= hand.getCardCount()) {
            throw new IndexOutOfBoundsException("Ungueltiger Kartenindex: " + index);
        }
        return hand.removeCard(index);
    }

    public void resetRoles() {
        this.isAttacker = false;
        this.isDefender = false;
    }
}
package CardGamesProject.src;

public final class DurakDeckFactory {
    private DurakDeckFactory() {
    }

    public static Deck create36CardDeck() {
        Deck deck = new Deck();
        deck.clear();
        Rank[] durakRanks = {
            Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN,
            Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE
        };
        for (Suit suit : Suit.values()) {
            for (Rank rank : durakRanks) {
                deck.addCard(new Card(suit, rank));
            }
        }
        deck.shuffle();
        return deck;
    }
}
package CardGamesProject.src;

import java.util.List;

public final class DurakRules {
    private DurakRules() {
    }

    public static boolean canDefend(Card attackCard, Card defenseCard, Suit trumpSuit) {
        if (attackCard == null || defenseCard == null || trumpSuit == null) {
            throw new IllegalArgumentException("Karten und Trumpffarbe duerfen nicht null sein.");
        }
        if (defenseCard.getSuit() == trumpSuit && attackCard.getSuit() != trumpSuit) {
            return true;
        }
        if (defenseCard.getSuit() == attackCard.getSuit()) {
            return defenseCard.getRank().ordinal() > attackCard.getRank().ordinal();
        }
        return false;
    }

    public static boolean canAttackWithCard(Card card, List<Card> tableCards) {
        if (card == null || tableCards == null) {
            throw new IllegalArgumentException("Parameter duerfen nicht null sein.");
        }
        if (tableCards.isEmpty()) {
            return true;
        }
        for (Card tableCard : tableCards) {
            if (tableCard.getRank() == card.getRank()) {
                return true;
            }
        }
        return false;
    }
}
package CardGamesProject.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DurakTable {
    private final List<Card> attackCards;
    private final List<Card> defenseCards;

    public DurakTable() {
        this.attackCards = new ArrayList<>();
        this.defenseCards = new ArrayList<>();
    }

    public void addAttackCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Karte darf nicht null sein.");
        }
        attackCards.add(card);
    }

    public void addDefenseCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Karte darf nicht null sein.");
        }
        defenseCards.add(card);
    }

    public List<Card> getAllTableCards() {
        List<Card> combined = new ArrayList<>(attackCards);
        combined.addAll(defenseCards);
        return Collections.unmodifiableList(combined);
    }

    public List<Card> getAttackCards() {
        return Collections.unmodifiableList(attackCards);
    }

    public List<Card> getDefenseCards() {
        return Collections.unmodifiableList(defenseCards);
    }

    public boolean isAllDefended() {
        return !attackCards.isEmpty() && attackCards.size() == defenseCards.size();
    }

    public void clear() {
        attackCards.clear();
        defenseCards.clear();
    }
}
package CardGamesProject.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DurakGame implements PlayableGame {
    public static final int HAND_SIZE = 6;
    private final List<DurakPlayer> players;
    private final Deck deck;
    private final DurakTable table;
    private Suit trumpSuit;
    private Card trumpCard;
    private int attackerIndex;
    private int defenderIndex;
    private boolean active;

    public DurakGame(List<DurakPlayer> players) {
        if (players == null || players.size() < 2 || players.size() > 6) {
            throw new IllegalArgumentException("Durak erfordert zwischen 2 und 6 Spieler.");
        }
        this.players = new ArrayList<>(players);
        this.deck = DurakDeckFactory.create36CardDeck();
        this.table = new DurakTable();
        this.active = false;
    }

    @Override
    public void start() {
        active = true;
        try {
            trumpCard = deck.drawCard();
            trumpSuit = trumpCard.getSuit();
            dealInitialCards();
        } catch (EmptyDeckException e) {
            throw new IllegalStateException("Unerwarteter Fehler bei Spielbeginn: Deck leer.", e);
        }
        attackerIndex = determineFirstAttacker();
        defenderIndex = (attackerIndex + 1) % players.size();
        updatePlayerRoles();
    }

    private void dealInitialCards() throws EmptyDeckException {
        for (int i = 0; i < HAND_SIZE; i++) {
            for (DurakPlayer player : players) {
                if (deck.hasCards()) {
                    player.getHand().addCard(deck.drawCard());
                }
            }
        }
    }

    private int determineFirstAttacker() {
        int startingPlayer = 0;
        Rank lowestTrumpRank = null;
        for (int i = 0; i < players.size(); i++) {
            for (Card c : players.get(i).getHand().getCards()) {
                if (c.getSuit() == trumpSuit) {
                    if (lowestTrumpRank == null || c.getRank().ordinal() < lowestTrumpRank.ordinal()) {
                        lowestTrumpRank = c.getRank();
                        startingPlayer = i;
                    }
                }
            }
        }
        return startingPlayer;
    }

    public void attack(DurakPlayer player, Card card) {
        if (!active || player != players.get(attackerIndex)) {
            throw new IllegalStateException("Spieler ist nicht am Zug fuer einen Angriff.");
        }
        if (!DurakRules.canAttackWithCard(card, table.getAllTableCards())) {
            throw new IllegalArgumentException("Karte passt nicht zu den ausliegenden Kartenwerten.");
        }
        player.getHand().getCards().remove(card);
        table.addAttackCard(card);
    }

    public void defend(DurakPlayer defender, Card defenseCard) {
        if (!active || defender != players.get(defenderIndex)) {
            throw new IllegalStateException("Spieler ist nicht der aktuelle Verteidiger.");
        }
        int attackIdx = table.getDefenseCards().size();
        if (attackIdx >= table.getAttackCards().size()) {
            throw new IllegalStateException("Kein offener Angriff vorhanden.");
        }
        Card attackCard = table.getAttackCards().get(attackIdx);
        if (!DurakRules.canDefend(attackCard, defenseCard, trumpSuit)) {
            throw new IllegalArgumentException("Abwehrkarte kann den Angriff nicht schlagen.");
        }
        defender.getHand().getCards().remove(defenseCard);
        table.addDefenseCard(defenseCard);
    }

    public void finishBoutSuccessfully() {
        if (!table.isAllDefended()) {
            throw new IllegalStateException("Runde kann nicht abgeschlossen werden: Unverteidigte Karten.");
        }
        table.clear();
        replenishHands();
        attackerIndex = defenderIndex;
        defenderIndex = getNextActivePlayerIndex(attackerIndex);
        updatePlayerRoles();
        checkGameStatus();
    }

    public void defenderTakesAll() {
        DurakPlayer defender = players.get(defenderIndex);
        for (Card c : table.getAllTableCards()) {
            defender.getHand().addCard(c);
        }
        table.clear();
        replenishHands();
        attackerIndex = getNextActivePlayerIndex(defenderIndex);
        defenderIndex = getNextActivePlayerIndex(attackerIndex);
        updatePlayerRoles();
        checkGameStatus();
    }

    private void replenishHands() {
        int current = attackerIndex;
        for (int i = 0; i < players.size(); i++) {
            DurakPlayer p = players.get(current);
            while (p.getHand().getCardCount() < HAND_SIZE && deck.hasCards()) {
                try {
                    p.getHand().addCard(deck.drawCard());
                } catch (EmptyDeckException ignored) {
                    break;
                }
            }
            if (p.getHand().getCardCount() < HAND_SIZE && !deck.hasCards() && trumpCard != null) {
                p.getHand().addCard(trumpCard);
                trumpCard = null;
            }
            current = (current + 1) % players.size();
        }
    }

    private int getNextActivePlayerIndex(int startIndex) {
        int next = (startIndex + 1) % players.size();
        int loopGuard = 0;
        while (!players.get(next).hasCards() && loopGuard < players.size()) {
            next = (next + 1) % players.size();
            loopGuard++;
        }
        return next;
    }

    private void updatePlayerRoles() {
        for (DurakPlayer p : players) {
            p.resetRoles();
        }
        if (active) {
            players.get(attackerIndex).setAttacker(true);
            players.get(defenderIndex).setDefender(true);
        }
    }

    private void checkGameStatus() {
        int activeCount = 0;
        for (DurakPlayer p : players) {
            if (p.hasCards()) {
                activeCount++;
            }
        }
        if (activeCount <= 1) {
            endGame();
        }
    }

    public DurakPlayer getDurak() {
        if (active) {
            return null;
        }
        for (DurakPlayer p : players) {
            if (p.hasCards()) {
                return p;
            }
        }
        return null;
    }

    public Suit getTrumpSuit() {
        return trumpSuit;
    }

    public DurakTable getTable() {
        return table;
    }

    public List<DurakPlayer> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    @Override
    public boolean isGameOver() {
        return !active;
    }

    @Override
    public void endGame() {
        this.active = false;
        for (DurakPlayer p : players) {
            p.resetRoles();
        }
    }
}
package CardGamesProject.src;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DurakTestRunner {
    public static void main(String[] args) {
        testDefenseRules();
        testAttackMatchingRules();
        testDeckGeneration();
        testGameFlowAndResolution();
        System.out.println("Alle Durak-Tests erfolgreich abgeschlossen.");
    }

    private static void testDefenseRules() {
        Suit trump = Suit.HEARTS;
        Card attackCard = new Card(Suit.SPADES, Rank.SEVEN);
        Card higherSameSuit = new Card(Suit.SPADES, Rank.TEN);
        Card lowerSameSuit = new Card(Suit.SPADES, Rank.SIX);
        Card trumpCard = new Card(Suit.HEARTS, Rank.SIX);
        Card wrongSuit = new Card(Suit.CLUBS, Rank.ACE);

        if (!DurakRules.canDefend(attackCard, higherSameSuit, trump)) {
            throw new AssertionError("Hoehere Karte gleicher Farbe muss stechen koennen.");
        }
        if (DurakRules.canDefend(attackCard, lowerSameSuit, trump)) {
            throw new AssertionError("Niedrigere Karte gleicher Farbe darf nicht stechen.");
        }
        if (!DurakRules.canDefend(attackCard, trumpCard, trump)) {
            throw new AssertionError("Trumpfkarte muss Nicht-Trumpf stechen.");
        }
        if (DurakRules.canDefend(attackCard, wrongSuit, trump)) {
            throw new AssertionError("Fremde Nicht-Trumpffarbe darf nicht abwehren.");
        }
    }

    private static void testAttackMatchingRules() {
        List<Card> tableCards = new ArrayList<>();
        tableCards.add(new Card(Suit.SPADES, Rank.EIGHT));
        tableCards.add(new Card(Suit.HEARTS, Rank.EIGHT));

        Card match = new Card(Suit.DIAMONDS, Rank.EIGHT);
        Card nonMatch = new Card(Suit.DIAMONDS, Rank.NINE);

        if (!DurakRules.canAttackWithCard(match, tableCards)) {
            throw new AssertionError("Karte mit vorhandenem Rang muss nachlegbar sein.");
        }
        if (DurakRules.canAttackWithCard(nonMatch, tableCards)) {
            throw new AssertionError("Karte ohne passenden Rang darf nicht nachgelegt werden.");
        }
    }

    private static void testDeckGeneration() {
        Deck deck = DurakDeckFactory.create36CardDeck();
        int count = 0;
        while (deck.hasCards()) {
            try {
                deck.drawCard();
                count++;
            } catch (EmptyDeckException e) {
                break;
            }
        }
        if (count != 36) {
            throw new AssertionError("Durak-Deck muss exakt 36 Karten umfassen, tatsaechlich: " + count);
        }
    }

    private static void testGameFlowAndResolution() {
        DurakPlayer p1 = new DurakPlayer("p1", "Anna");
        DurakPlayer p2 = new DurakPlayer("p2", "Ben");
        DurakGame game = new DurakGame(Arrays.asList(p1, p2));
        game.start();

        if (game.getTrumpSuit() == null) {
            throw new AssertionError("Trumpffarbe wurde nicht initialisiert.");
        }
        if (p1.getHand().getCardCount() != DurakGame.HAND_SIZE || p2.getHand().getCardCount() != DurakGame.HAND_SIZE) {
            throw new AssertionError("Spieler muessen zu Rundenbeginn 6 Karten besitzen.");
        }
    }
}