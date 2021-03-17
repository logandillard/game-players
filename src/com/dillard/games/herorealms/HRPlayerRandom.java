package com.dillard.games.herorealms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HRPlayerRandom implements HRPlayer {
    private Random rand;

    public HRPlayerRandom(long seed) {
        rand = new Random(seed);
    }

    @Override
    public String getName() {
        return "RandomPlayer";
    }

    private Card chooseRandomCard(List<Card> cards) {
        List<Card> choices = new ArrayList<>(cards);
        choices.add(null);
        int idx = rand.nextInt(choices.size());
        return choices.get(idx);
    }

    @Override
    public Card chooseSacrifice(List<Card> cards, HRGameState gameState) {
        return chooseRandomCard(cards);
    }

    @Override
    public Card chooseBuy(List<Card> cards, HRGameState gameState, int gold) {
        return chooseRandomCard(cards);
    }

    @Override
    public Card chooseTuck(List<Card> cards, HRGameState gameState, int gold) {
        return chooseRandomCard(cards);
    }

    @Override
    public Card chooseSacForDamage(List<Card> cards, HRGameState gameState) {
        return chooseRandomCard(cards);
    }
}
