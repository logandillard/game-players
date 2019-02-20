package com.dillard.games.checkers;

import java.util.Random;

public class RandomCheckersPlayer implements CheckersPlayer {
    private Random rand;

    public RandomCheckersPlayer(Random rand) {
        this.rand = rand;
    }

    @Override
    public CheckersMove move(CheckersGame game) {
        var moves = game.getMoves();
        return moves.get(rand.nextInt(moves.size()));
    }

}
