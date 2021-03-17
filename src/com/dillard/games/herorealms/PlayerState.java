package com.dillard.games.herorealms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlayerState {
    final int INITIAL_HEALTH = 50;

    private int health;
    private List<Card> library;
    private List<Card> allCards = new ArrayList<>();
    private List<Card> discardPile = new ArrayList<>();
    private List<Card> champions = new ArrayList<>();
    private List<Card> tuckedCards = new ArrayList<>();
    private List<Card> sacrificedCards = new ArrayList<>();
    private Random random;

    public PlayerState(Random rand) {
        this.random = rand;
        health = INITIAL_HEALTH;
        library = Cards.newLibrary();
        allCards.addAll(library);
        Collections.shuffle(library, random);
    }

    public List<Card> drawCards(int cardsToDraw) {
        List<Card> hand = new ArrayList<>();
        while (hand.size() < cardsToDraw) {

            if (library.isEmpty()) {
                shuffle();
            }

            if (library.isEmpty()) {
                // no cards to draw!
                break;
            }

            hand.add(library.remove(library.size() - 1));
        }
        return hand;
    }

    public void shuffle() {
        library.addAll(discardPile);
        discardPile = new ArrayList<>();
        Collections.shuffle(library, random);
    }

    public void addCard(Card c) {
        discardPile.add(c);
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int h) {
        health = h;
    }

    public void discard(List<Card> cards) {
        discardPile.addAll(cards);
    }

    public List<Card> getChampions() {
        return champions;
    }

    public void setChampions(List<Card> c) {
        champions = c;
    }

    public void discardChampion(Card championToStun) {
        boolean wasPresent = champions.remove(championToStun);
        if (!wasPresent) {
            throw new RuntimeException();
        }
        discardPile.add(championToStun);
    }

    public List<Card> getDiscardPile() {
        return discardPile;
    }

    public List<Card> getLibrary() {
        return library;
    }

    public void putOnTopOfLibrary(Card card) {
        library.add(card);
    }

    public void addTuckedCard(Card cardToTuck) {
        tuckedCards.add(cardToTuck);
    }

    public List<Card> getTuckedCards() {
        return tuckedCards;
    }

    public void clearTuckedCards() {
        tuckedCards = new ArrayList<>();
    }

    public void addSacrificed(Card toSac) {
        sacrificedCards.add(toSac);
        allCards.remove(toSac);
    }

    public void addBoughtCard(Card card) {
        allCards.add(card);
    }

    public List<Card> getAllCards() {
        return allCards;
    }
}
