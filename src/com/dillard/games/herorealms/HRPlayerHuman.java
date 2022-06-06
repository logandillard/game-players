package com.dillard.games.herorealms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class HRPlayerHuman implements HRPlayer {
    private String name;

    public HRPlayerHuman(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Card chooseSacrifice(List<Card> discardPile, HRGameState gameState) {
        print("");
        printGameState(gameState);
        print("Choose a card to sacrifice");
        return chooseCard(discardPile);
    }

    @Override
    public Card chooseSacForDamage(List<Card> sacableCards, HRGameState gameState) {
        print("");
        printGameState(gameState);
        print("Choose a card to sacrifice for damage");
        return chooseCard(sacableCards);
    }

    @Override
    public Card chooseBuy(List<Card> cardsToBuy, HRGameState gameState, int gold) {
        print("");
        print(name);
        printGameState(gameState);
        print("Opponent champions: " + gameState.getChampions(gameState.currentOpponent()));
        print("Choose a card to buy");
        print("Gold: " + gold);
        print("Market:");
        print(gameState.getMarket().toString());
        return chooseCard(cardsToBuy);
    }

    @Override
    public Card chooseTuck(List<Card> cardsToTuck, HRGameState gameState, int gold) {
        print("");
        printGameState(gameState);
        print("Choose a card to tuck");
        print("Gold: " + gold);
        print("Market:");
        print(gameState.getMarket().toString());
        return chooseCard(cardsToTuck);
    }

    private void printGameState(HRGameState gameState) {
        print("Your health " + gameState.getPlayerHealth(this) +
                "  |  Opponent health: " + gameState.getPlayerHealth(gameState.currentOpponent()));
    }

    public Card chooseCard(List<Card> cards) {
        print("0   NONE");
        for (int i=0; i<cards.size(); i++) {
            print((i+1) + "   " + cards.get(i).toStringFull());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        do {
            print("Enter card number");
            try {
                String input = reader.readLine();
                if (input.isEmpty()) continue;

                int choice = Integer.parseInt(input);
                if (choice < 0 || choice > cards.size()) {
                    continue;
                }

                if (choice == 0) {
                    return null;
                }
                return cards.get(choice - 1);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } while (true);
    }

    private void print(String s) {
        System.out.println(s);
    }

//    private String pad(int i, String s) {
//        StringBuilder sb = new StringBuilder(s);
//        while (sb.length() < i) {
//            sb.append(" ");
//        }
//        return sb.toString();
//    }
}
