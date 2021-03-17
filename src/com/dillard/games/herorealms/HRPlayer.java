package com.dillard.games.herorealms;

import java.util.List;

public interface HRPlayer {

    String getName();

    Card chooseSacrifice(List<Card> discardPile, HRGameState gameState);

    Card chooseBuy(List<Card> cardsToBuy, HRGameState gameState, int gold);

    Card chooseTuck(List<Card> cardsToTuck, HRGameState gameState, int gold);

    Card chooseSacForDamage(List<Card> sacableCards, HRGameState gameState);
}
