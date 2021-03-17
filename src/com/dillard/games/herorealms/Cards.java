package com.dillard.games.herorealms;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dillard.games.herorealms.HRGame.CardWrapper;

public class Cards {
    public static final int NUM_UNIQUE_CARDS = 60;
    public static final Card GOLD = new Card("gold", 0, CardColor.NONE, false, false, 0, 0, Map.of("gold", 1));
    public static final Card DAGGER = new Card("dagger", 0, CardColor.NONE, false, false, 0, 1, Map.of("damage", 1));
    public static final Card SHORT_SWORD = new Card("short_sword", 0, CardColor.NONE, false, false, 0, 2, Map.of("damage", 2));
    public static final Card RUBY = new Card("ruby", 0, CardColor.NONE, false, false, 0, 3, Map.of("gold", 2));
    public static final Card FIREGEM = new Card("firegem", 2, CardColor.NONE, false, false, 0, 4, Map.of(
            "gold", 2,
            "sacDamage", 3
            ));

    private static List<Card> MARKET_CARDS = null;

    public static List<Card> newLibrary() {
        List<Card> startingCards = new ArrayList<>();
        for (int i=0; i<7; i++) {
            startingCards.add(Cards.GOLD);
        }
        startingCards.add(DAGGER);
        startingCards.add(SHORT_SWORD);
        startingCards.add(RUBY);

        return startingCards;
    }

    public static List<Card> loadMarketCards() {
        if (MARKET_CARDS != null) return new ArrayList<>(MARKET_CARDS);

        List<Card> cards = new ArrayList<>();
        int ordinal = FIREGEM.ordinal + 1;
        try {
            URL url = Cards.class.getResource("/hero_realms_cards.csv");
            Path p = Paths.get(url.toURI());
            List<String> lines = Files.readAllLines(p);

            String[] header = null;
            for (int i=0; i<lines.size(); i++) {
                String line = lines.get(i);
                if (i == 0) {
                    header = line.split(",");
                    continue;
                }

                // Ignore these non-market cards
                if (line.startsWith("gold,") ||
                        line.startsWith("ruby,") ||
                        line.startsWith("dagger,") ||
                        line.startsWith("short sword,") ||
                        line.startsWith("fire gem,")
                        ) {
                    continue;
                }

//                if (line.startsWith("death touch")) {
//                    int bp = 1;
//                }

                String name = null;
                int count = 0;
                CardColor color = CardColor.NONE;
                int cost = 0;
                boolean isChampion = false;
                boolean isGuard = false;
                int championHealth = 0;
                Map<String,Integer> attributes = new HashMap<>();

                String[] cols = line.split(",");
                for (int c=0; c<cols.length; c++) {
                    String str = cols[c];
                    if (str.isEmpty()) continue;

                    // TODO handle ":or"
                    str = str.replace(":or", "");

                    // name,color,cost,count,champion,gold,syn_gold,damage,syn_damage,sac_damage,
                    // draw_card,syn_draw_card,health,syn_health,discard,syn_discard,sac_discard,
                    // sacrifice,draw_discard,syn_draw_discard,stun,syn_stun,*
                    switch (header[c]) {
                    case "name":
                        name = str;
                        break;
                    case "color":
                        color = CardColor.forString(str);
                        break;
                    case "cost":
                        cost = Integer.parseInt(str);
                        break;
                    case "count":
                        count = Integer.parseInt(str);
                        break;
                    case "champion":
                        isChampion = true;
                        if (str.startsWith("g")) {
                            isGuard = true;
                        }
                        championHealth = Integer.parseInt(str.substring(isGuard ? 1 : 0));
                        break;
                    case "gold":
                        attributes.put("gold", Integer.parseInt(str));
                        break;
                    case "syn_gold":
                        attributes.put("goldSyn", Integer.parseInt(str));
                        break;
                    case "damage":
                        // can contain 'c' for per champion or 'g' for per other green card or guard!

                        if (str.contains("c")) {
                            // "x+yc"
                            int standard = Integer.parseInt(str.substring(0, 1));
                            int perChamp = Integer.parseInt(str.substring(2, 3));
                            attributes.put("damage", standard);
                            attributes.put("damagePerChampion", perChamp);
                        } else if (str.contains("g")) {
                            // "x+yg"
                            int standard = Integer.parseInt(str.substring(0, 1));
                            int perThing = Integer.parseInt(str.substring(2, 3));
                            attributes.put("damage", standard);
                            if (color == CardColor.GREEN) {
                                attributes.put("damagePerGreen", perThing);
                            } else {
                                attributes.put("damagePerGuard", perThing);
                            }
                        } else {
                            attributes.put("damage", Integer.parseInt(str));
                        }
                        break;
                    case "syn_damage":
                        attributes.put("damageSyn", Integer.parseInt(str));
                        break;
                    case "sac_damage":
                        attributes.put("sacDamage", Integer.parseInt(str));
                        break;
                    case "draw_card":
                        attributes.put("cardsToDraw", Integer.parseInt(str));
                        break;
                    case "syn_draw_card":
                        attributes.put("cardsToDrawSyn", Integer.parseInt(str));
                        break;
                    case "health":
                        // can contain 'c' for per champion
                        if (str.contains("c")) {
                            // "x+yc"
                            int standard = Integer.parseInt(str.substring(0, 1));
                            int perChamp = Integer.parseInt(str.substring(2, 3));
                            attributes.put("health", standard);
                            attributes.put("healthPerChampion", perChamp);
                        } else {
                            attributes.put("health", Integer.parseInt(str));
                        }
                        break;
                    case "syn_health":
                        // can contain 'c' for per champion
                        if (str.contains("c")) {
                            // "x+yc"
                            int standard = Integer.parseInt(str.substring(0, 1));
                            int perChamp = Integer.parseInt(str.substring(2, 3));
                            attributes.put("healthSyn", standard);
                            attributes.put("healthPerChampionSyn", perChamp);
                        } else {
                            attributes.put("healthSyn", Integer.parseInt(str));
                        }
                        break;
                    case "discard":
                        attributes.put("opponentDiscard", Integer.parseInt(str));
                        break;
                    case "syn_discard":
                        attributes.put("opponentDiscardSyn", Integer.parseInt(str));
                        break;
                    case "sac_discard":
                        attributes.put("sacDiscard", Integer.parseInt(str));
                        break;
                    case "sacrifice":
                        attributes.put("sacrifice", Integer.parseInt(str));
                        break;
                    case "draw_discard":
                        attributes.put("drawThenDiscard", Integer.parseInt(str));
                        break;
                    case "syn_draw_discard":
                        attributes.put("drawThenDiscardSyn", Integer.parseInt(str));
                        break;
                    case "stun":
                        attributes.put("stunChampions", Integer.parseInt(str));
                        break;
                    case "syn_stun":
                        attributes.put("stunChampionsSyn", Integer.parseInt(str));
                        break;
                    case "*":
                        switch (str) {
                        case "card from discard pile on top of deck":
                            attributes.put("moveAnyFromDiscardToDeck", 1);
                            break;
                        case "champion from discard pile on top":
                            attributes.put("moveChampionFromDiscardToDeck", 1);
                            break;
                        case "synergy prepare a champion":
                            attributes.put("prepareChampionSyn", 1);
                            break;
                        case "next card on top": // this is actually synergy
                            attributes.put("nextBuyOnTopSyn", 1);
                            break;
                        case "next card in hand": // this is actually synergy
                            attributes.put("nextBuyInHandSyn", 1);
                            break;
                        case "next action on top": // this is actually synergy
                            attributes.put("nextActionOnTopSyn", 1);
                            break;
                        default:
                            throw new RuntimeException(str);
                        }
                        break;

                    default:
                        throw new RuntimeException(header[c] + " - " + str);
                    }
                }

                if (count < 1) {
                    System.out.println(line);
                    throw new RuntimeException("Got illegal count of " + count);
                }
                for (int c=0; c<count; c++) {
                    cards.add(new Card(
                            name, cost, color, isChampion,
                            isGuard, championHealth, ordinal, attributes
                            ));
                }
                ordinal++;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (Card c : cards) {
            // assert no cards have draw a card and synergy draw a card
            if (c.cardsToDraw(true) > c.cardsToDraw(false) && c.cardsToDraw(false) > 0) {
                throw new RuntimeException();
            }
            // assert no cards have draw a card and synergy draw+discard
            if (c.cardsToDraw(false) > 0 && c.drawThenDiscard(true) > 0) {
                throw new RuntimeException();
            }
        }

        MARKET_CARDS = cards;

        return new ArrayList<>(MARKET_CARDS);
    }

    public static int[] colorCounts(List<Card> cards) {
        int[] colorCount = new int[CardColor.NUM_VALUES];
        for (Card card : cards) {
            colorCount[card.color.ordinal()]++;
        }
        return colorCount;
    }

    public static final class DiscardComparator implements Comparator<Card> {
        public int compare(Card a, Card b) {
            return a.discardScore - b.discardScore;
        }
    }

    public static final class WrapperDiscardComparator implements Comparator<CardWrapper> {
        public int compare(CardWrapper wa, CardWrapper wb) {
            Card a = wa.card;
            Card b = wb.card;
            return a.discardScore - b.discardScore;
        }
    }

    public static final class CostComparator implements Comparator<Card> {
        public int compare(Card a, Card b) {
            // use cost, except make firegems rank below other two-cost cards
            int aCost = a.cost + (a.cost >= 2 && !a.name.equals(Cards.FIREGEM.name) ? 1 : 0);
            int bCost = b.cost + (b.cost >= 2 && !b.name.equals(Cards.FIREGEM.name) ? 1 : 0);
            return aCost - bCost;
        }
    }

    public static final class WrapperCostComparator implements Comparator<CardWrapper> {
        public int compare(CardWrapper wa, CardWrapper wb) {
            Card a = wa.card;
            Card b = wb.card;
            // use cost, except make firegems rank below other two-cost cards
            int aCost = a.cost + (a.cost >= 2 && !a.name.equals(Cards.FIREGEM.name) ? 1 : 0);
            int bCost = b.cost + (b.cost >= 2 && !b.name.equals(Cards.FIREGEM.name) ? 1 : 0);
            return aCost - bCost;
        }
    }

    public static class ChampionToStunComparator implements Comparator<Card> {
        public int compare(Card a, Card b) {
            return a.championHealth - b.championHealth;
        }
    }

}
