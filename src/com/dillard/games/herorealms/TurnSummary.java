package com.dillard.games.herorealms;

import java.util.ArrayList;
import java.util.List;

public class TurnSummary {
    public final int cardsToDraw;
    public final int drawThenDiscard;
    public final int stunChampions;
    public final int damage;
    public final int opponentDiscard;
    public final int health;
    public final int gold;
    public final int sacrifice;
    public final int moveAnyFromDiscardToDeck;
    public final int moveChampionFromDiscardToDeck;
    public final int prepareChampions;
    public final int nextBuyOnTop;
    public final int nextBuyInHand;
    public final int nextActionOnTop;
    public final int[] colorCount;

    public TurnSummary(int cardsToDraw, int drawThenDiscard, int stunChampions, int damage, int opponentDiscard,
            int health, int gold, int moveAnyFromDiscardToDeck, int moveChampionFromDiscardToDeck, int sacrifice,
            int prepareChampions, int nextBuyOnTop, int nextBuyInHand, int nextActionOnTop, int[] colorCount) {
        this.cardsToDraw = cardsToDraw;
        this.drawThenDiscard = drawThenDiscard;
        this.stunChampions = stunChampions;
        this.damage = damage;
        this.opponentDiscard = opponentDiscard;
        this.health = health;
        this.gold = gold;
        this.sacrifice = sacrifice;
        this.moveAnyFromDiscardToDeck = moveAnyFromDiscardToDeck;
        this.moveChampionFromDiscardToDeck = moveChampionFromDiscardToDeck;
        this.prepareChampions = prepareChampions;
        this.nextBuyOnTop = nextBuyOnTop;
        this.nextBuyInHand = nextBuyInHand;
        this.nextActionOnTop = nextActionOnTop;
        this.colorCount = colorCount;
    }

    public static TurnSummary calculateTurnSummary(List<Card> hand, List<Card> playedCards) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(hand);
        allCards.addAll(playedCards);
        return TurnSummary.calculateTurnSummary(allCards);
    }

    public static TurnSummary calculateTurnSummary(List<Card> hand) {
        int[] colorCount = new int[CardColor.NUM_VALUES];
        for (Card card : hand) {
            colorCount[card.color.ordinal()]++;
        }

        int cardsToDraw = 0;
        int drawThenDiscard = 0;
        int stunChampions = 0;
        int damage = 0;
        int opponentDiscard = 0;
        int health = 0;
        int gold = 0;
        int sacrifice = 0;
        int moveAnyFromDiscardToDeck = 0;
        int moveChampionFromDiscardToDeck = 0;
        int prepareChampions = 0;
        int nextBuyOnTop = 0;
        int nextBuyInHand = 0;
        int nextActionOnTop = 0;

        for (Card card : hand) {
            boolean synergy = colorCount[card.color.ordinal()] > 1 ? true : false;
            cardsToDraw += card.cardsToDraw(synergy);
            drawThenDiscard += card.drawThenDiscard(synergy);
            stunChampions += card.stunChampions(synergy);
            damage += card.damage(synergy, hand);
            opponentDiscard += card.opponentDiscard(synergy);
            health += card.health(synergy, hand);
            gold += card.gold(synergy, hand);
            moveAnyFromDiscardToDeck += card.moveAnyFromDiscardToDeck(synergy);
            moveChampionFromDiscardToDeck += card.moveChampionFromDiscardToDeck(synergy);
            sacrifice += card.sacrifice(synergy);
            prepareChampions += card.prepareChampions(synergy);
            nextBuyOnTop += card.nextBuyOnTop(synergy);
            nextBuyInHand += card.nextBuyInHand(synergy);
            nextActionOnTop += card.nextActionOnTop(synergy);
        }

        return new TurnSummary(
            cardsToDraw, drawThenDiscard, stunChampions, damage, opponentDiscard,
            health, gold, moveAnyFromDiscardToDeck, moveChampionFromDiscardToDeck, sacrifice,
            prepareChampions, nextBuyOnTop, nextBuyInHand, nextActionOnTop, colorCount
        );
    }

    public TurnSummary plusPreparedChampion(Card champ, List<Card> hand) {
        return new TurnSummary(
                cardsToDraw + champ.cardsToDraw(false),
                drawThenDiscard + champ.drawThenDiscard(false),
                stunChampions + champ.stunChampions(false),
                damage + champ.damage(false, hand),
                opponentDiscard + champ.opponentDiscard(false),
                health + champ.health(false, hand),
                gold + champ.gold(false, hand),
                moveAnyFromDiscardToDeck + champ.moveAnyFromDiscardToDeck(false),
                moveChampionFromDiscardToDeck + champ.moveChampionFromDiscardToDeck(false),
                sacrifice + champ.sacrifice(false),
                prepareChampions + champ.prepareChampions(false),
                nextBuyOnTop + champ.nextBuyOnTop(false),
                nextBuyInHand + champ.nextBuyInHand(false),
                nextActionOnTop + champ.nextActionOnTop(false),
                colorCount
            );
    }
}
