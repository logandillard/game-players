package com.dillard.games.herorealms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HRGameState {

    final int MARKET_SIZE = 5;

    private int turnNumber = 0;
    private Random random;
    private List<HRPlayer> players;
    private List<Card> marketDeck;
    private List<Card> market = new ArrayList<>();
    private List<Card> tuckSacrificedCards = new ArrayList<>();
    private Map<String, PlayerState> playerState = new HashMap<>();
    private int discardCardCount = 0;
    private int currentPlayerIndex = 0;

    public HRGameState(List<HRPlayer> players, long randomSeed) {
        this.players = players;
        random = new Random(randomSeed);

        initPlayers();

        // init market
        marketDeck = Cards.loadMarketCards();
        Collections.shuffle(marketDeck, random);
        drawMarket(null);
    }

    private void initPlayers() {
        for (HRPlayer player : players) {
            playerState.put(player.getName(), new PlayerState(random));
        }
    }

    public void drawMarket(HRPlayer player) {
        // remove tucked cards
        if (player != null) {
            for (Card card : playerState(player).getTuckedCards()) {
                market.remove(card);
            }
            playerState(player).clearTuckedCards();
        }
        while (market.size() < MARKET_SIZE && !marketDeck.isEmpty()) {
            market.add(marketDeck.remove(marketDeck.size() - 1));
        }
    }

    public List<Card> drawCards(HRPlayer player, int cardsToDraw) {
        return playerState(player).drawCards(cardsToDraw);
    }

    public void discard(HRPlayer player, List<Card> cards) {
        playerState(player).discard(cards);
    }

    public void incrementTurnNumber() {
        turnNumber++;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public int getPlayerHealth(HRPlayer player) {
        return playerState(player).getHealth();
    }

    public void damagePlayer(HRPlayer player, int damage) {
        PlayerState ps = playerState(player);
        int startingHealth = ps.getHealth();
        ps.setHealth(startingHealth - damage);
    }

    public void startTurn() {
        // TODO Auto-generated method stub

    }

    public void endTurn() {
        currentPlayerIndex = (currentPlayerIndex+1) % players.size();
    }

    public HRPlayer currentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public HRPlayer currentOpponent() {
        return players.get((currentPlayerIndex+1) % players.size());
    }

    public void clearDiscardCardCount() {
        discardCardCount = 0;
    }

    public int discardCardCount() {
        return discardCardCount;
    }

    public void setDiscardCardCount(int opponentDiscard) {
        this.discardCardCount = opponentDiscard;
    }

    public List<Card> getChampions(HRPlayer player) {
        return playerState(player).getChampions();
    }

    public void setChampions(HRPlayer player, List<Card> champions) {
        playerState(player).setChampions(champions);
    }

    public void stunChampion(HRPlayer player, Card championToStun) {
        playerState(player).discardChampion(championToStun);
    }

    public void gainHealth(HRPlayer player, int health) {
        PlayerState ps = playerState(player);
        int startingHealth = ps.getHealth();
        ps.setHealth(startingHealth + health);
    }

    private PlayerState playerState(HRPlayer player) {
        return playerState.get(player.getName());
    }

    public List<Card> getDiscardPile(HRPlayer player) {
        return playerState(player).getDiscardPile();
    }

    public void putOnTopOfLibrary(HRPlayer player, Card card) {
        playerState(player).putOnTopOfLibrary(card);
    }

    public List<Card> getLibrary(HRPlayer player) {
        return playerState(player).getLibrary();
    }

    public void addPlayerSacrificed(HRPlayer player, Card toSac) {
        playerState(player).addSacrificed(toSac);
    }

    public void addTuckSacrificed(Card toSac) {
        tuckSacrificedCards.add(toSac);
    }

    public List<Card> getMarket() {
        return market;
    }

    public void addTuckedCard(HRPlayer player, Card cardToTuck) {
        playerState(player).addTuckedCard(cardToTuck);
    }

    public List<Card> getTuckedCards(HRPlayer player) {
        return playerState(player).getTuckedCards();
    }

    public void addBoughtCard(HRPlayer player, Card card) {
        playerState(player).addBoughtCard(card);
    }

    public List<Card> getAllPlayerCards(HRPlayer player) {
        return playerState(player).getAllCards();
    }
}
