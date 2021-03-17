package com.dillard.games.herorealms;

import java.util.List;
import java.util.Map;

public class Card {
    public final int discardScore;
    public final String name;
    public final int cost;
    public final CardColor color;
    public final boolean isChampion;
    public final boolean isGuard;
    public final int championHealth;
    private Map<String, Integer> attributes;

    public Card(String name, int cost, CardColor color, boolean isChampion, boolean isGuard, int championHealth,
            int ordinal, Map<String, Integer> attributes) {
        this.name = name;
        this.ordinal = ordinal;
        this.cost = cost;
        this.color = color;
        this.isChampion = isChampion;
        this.isGuard = isGuard;
        this.championHealth = championHealth;
        discardScore = discardScore(name, cost);

        this.attributes = attributes;
        for (Map.Entry<String, Integer> entry : attributes.entrySet()) {
            String attr = entry.getKey();
            Integer v = entry.getValue();

            switch (attr) {
            case "damage":
                damage = v;
                break;
            case "damageSyn":
                damageSyn = v;
                break;
            case "damagePerChampion":
                damagePerChampion = v;
                break;
            case "damagePerGreen":
                damagePerGreen = v;
                break;
            case "damagePerGuard":
                damagePerGuard = v;
                break;
            case "sacDamage":
                sacDamage = v;
                break;
            case "gold":
                gold = v;
                break;
            case "goldSyn":
                goldSyn = v;
                break;
            case "health":
                health = v;
                break;
            case "healthSyn":
                healthSyn = v;
                break;
            case "healthPerChampion":
                healthPerChampion = v;
                break;
            case "healthPerChampionSyn":
                healthPerChampionSyn = v;
                break;
            case "cardsToDraw":
                cardsToDraw = v;
                break;
            case "cardsToDrawSyn":
                cardsToDrawSyn = v;
                break;
            case "drawThenDiscard":
                drawThenDiscard = v;
                break;
            case "drawThenDiscardSyn":
                drawThenDiscardSyn = v;
                break;
            case "stunChampions":
                stunChampions = v;
                break;
            case "stunChampionsSyn":
                stunChampionsSyn = v;
                break;
            case "sacrifice":
                sacrifice = v;
                break;
            case "sacrificeSyn":
                sacrificeSyn = v;
                break;
            case "opponentDiscard":
                opponentDiscard = v;
                break;
            case "opponentDiscardSyn":
                opponentDiscardSyn = v;
                break;
            case "sacDiscard":
                sacDiscard = v;
                break;
            case "moveAnyFromDiscardToDeck":
                moveAnyFromDiscardToDeck = v;
                break;
            case "moveAnyFromDiscardToDeckSyn":
                moveAnyFromDiscardToDeckSyn = v;
                break;
            case "moveChampionFromDiscardToDeck":
                moveChampionFromDiscardToDeck = v;
                break;
            case "moveChampionFromDiscardToDeckSyn":
                moveChampionFromDiscardToDeckSyn = v;
                break;
            case "prepareChampionSyn":
                prepareChampionSyn = v;
                break;
            case "nextBuyOnTopSyn":
                nextBuyOnTopSyn = v;
                break;
            case "nextBuyInHandSyn":
                nextBuyInHandSyn = v;
                break;
            case "nextActionOnTopSyn":
                nextActionOnTopSyn = v;
                break;
            default:
                throw new RuntimeException(entry.toString());
            }
        }
    }

    private int damage = 0;
    private int damageSyn = 0;
    private int damagePerChampion = 0;
    private int damagePerGreen = 0;
    private int damagePerGuard = 0;
    private int sacDamage = 0;
    private int gold = 0;
    private int goldSyn = 0;
    private int health = 0;
    private int healthSyn = 0;
    private int healthPerChampion = 0;
    private int healthPerChampionSyn = 0;
    private int cardsToDraw = 0;
    private int cardsToDrawSyn = 0;
    private int drawThenDiscard = 0;
    private int drawThenDiscardSyn = 0;
    private int stunChampions = 0;
    private int stunChampionsSyn = 0;
    private int sacrifice = 0;
    private int sacrificeSyn = 0;
    private int opponentDiscard = 0;
    private int opponentDiscardSyn = 0;
    private int sacDiscard = 0;
    private int moveAnyFromDiscardToDeck = 0;
    private int moveAnyFromDiscardToDeckSyn = 0;
    private int moveChampionFromDiscardToDeck = 0;
    private int moveChampionFromDiscardToDeckSyn = 0;
    private int prepareChampionSyn = 0;
    private int nextBuyOnTopSyn = 0;
    private int nextBuyInHandSyn = 0;
    private int nextActionOnTopSyn = 0;
    public int ordinal;

    public int damage(boolean synergy, List<Card> hand) {
        int totalDamage = doSyn(synergy, damage, damageSyn);
        if (damagePerChampion > 0) {
            int champCount = (int) hand.stream().filter(c -> c.isChampion).count();
            totalDamage += healthPerChampion * (champCount - 1);
        }
        if (damagePerGreen > 0) {
            int greenCount = (int) hand.stream().filter(c -> c.color == CardColor.GREEN).count();
            totalDamage += healthPerChampion * (greenCount - 1);
        }
        if (damagePerGuard > 0) {
            int guardCount = (int) hand.stream().filter(c -> c.isGuard).count();
            totalDamage += healthPerChampion * (guardCount - 1);
        }
        return totalDamage;
    }

    public int gold(boolean synergy, List<Card> hand) {
        return doSyn(synergy, gold, goldSyn);
    }

    public int health(boolean synergy, List<Card> hand) {
        int totalHealth = doSyn(synergy, health, healthSyn);
        if (healthPerChampion > 0) {
            int champCount = (int) hand.stream().filter(c -> c.isChampion).count();
            totalHealth += healthPerChampion * champCount;
        }
        if (synergy && healthPerChampionSyn > 0) {
            int champCount = (int) hand.stream().filter(c -> c.isChampion).count();
            totalHealth += healthPerChampionSyn * (champCount - 1);
        }
        return totalHealth;
    }

    public int cardsToDraw(boolean synergy) {
        return doSyn(synergy, cardsToDraw, cardsToDrawSyn);
    }

    public int drawThenDiscard(boolean synergy) {
        return doSyn(synergy, drawThenDiscard, drawThenDiscardSyn);
    }

    public int stunChampions(boolean synergy) {
        return doSyn(synergy, stunChampions, stunChampionsSyn);
    }

    public int sacrifice(boolean synergy) {
        return doSyn(synergy, sacrifice, sacrificeSyn);
    }

    public int opponentDiscard(boolean synergy) {
        return doSyn(synergy, opponentDiscard, opponentDiscardSyn);
    }

    public int sacrificeDamage() {
        return sacDamage;
    }

    public int sacrificeDiscard() {
        return sacDiscard;
    }

    public int moveAnyFromDiscardToDeck(boolean synergy) {
        return doSyn(synergy, moveAnyFromDiscardToDeck, moveAnyFromDiscardToDeckSyn);
    }

    public int moveChampionFromDiscardToDeck(boolean synergy) {
        return doSyn(synergy, moveChampionFromDiscardToDeck, moveChampionFromDiscardToDeckSyn);
    }

    public int prepareChampions(boolean synergy) {
        return synergy ? prepareChampionSyn : 0;
    }

    public int nextBuyOnTop(boolean synergy) {
        return synergy ? nextBuyOnTopSyn : 0;
    }

    public int nextBuyInHand(boolean synergy) {
        return synergy ? nextBuyInHandSyn : 0;
    }

    public int nextActionOnTop(boolean synergy) {
        return synergy ? nextActionOnTopSyn : 0;
    }

    private int discardScore(String name, int cost) {
        if (name == "gold") {
            return 0;
        } else if (name == "dagger") {
            return 1;
        } else if (name == "short sword") {
            return 2;
        } else if (name == "ruby") {
            return 3;
        } else {
            return 3 + cost;
        }
    }

    private int doSyn(boolean synergy, int plain, int syn) {
        return synergy ? plain + syn : plain;
    }

    public boolean isAction() {
        return cost > 0 && !isChampion && name != Cards.FIREGEM.name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String toStringFull() {
        return name + ": (" + cost + ") " + color + " " +
                (isChampion ? (isGuard ? "G" : "C") + championHealth  + " " : "") + attributes.toString();
    }
}
