package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.dillard.games.checkers.MCTS.MCTSPlayer;

public class RandomCheckersPlayer implements CheckersPlayer, MCTSPlayer<CheckersMove, CheckersGame> {
    private Random rand;

    public RandomCheckersPlayer(Random rand) {
        this.rand = rand;
    }

    @Override
    public CheckersMove move(CheckersGame game) {
        var moves = game.getMoves();
        return moves.get(rand.nextInt(moves.size()));
    }

    public StateEvaluation<CheckersMove> evaluateState(CheckersGame game) {
        double evaluation = 0.0;
        List<CheckersMove> moves = game.getMoves();
        List<Double> scores = new ArrayList<>();
        for (@SuppressWarnings("unused") CheckersMove move : moves) {
            scores.add(1.0 / moves.size());
        }
        return new StateEvaluation<>(evaluation, moves, scores);
    }

}
