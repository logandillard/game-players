package com.dillard.games.herorealms;

import java.util.Collections;
import java.util.List;

public class HRPlayerSimple implements HRPlayer {

    @Override
    public String getName() {
        return "SimpleAlgo";
    }

    @Override
    public Card chooseSacrifice(List<Card> discardPile, HRGameState gameState) {
        Collections.sort(discardPile, new Cards.DiscardComparator());
        Card c = discardPile.get(0);
        // Only sacrifice starting cards
        if (c.cost == 0) {
            return c;
        }
        return null;
    }

    @Override
    public Card chooseBuy(List<Card> cardsToBuy, HRGameState gameState, int gold) {
        Collections.sort(cardsToBuy, new Cards.CostComparator());
        Card c = cardsToBuy.get(cardsToBuy.size() - 1);
        return c;
    }

    @Override
    public Card chooseTuck(List<Card> cardsToTuck, HRGameState gameState, int gold) {
        return null;
    }

    @Override
    public Card chooseSacForDamage(List<Card> sacableCards, HRGameState gameState) {
        // nope
        return null;
    }
}
