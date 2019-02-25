package com.dillard.games.checkers;

import java.util.Random;

import com.dillard.games.checkers.MCTS.MCTSPlayer;

public class MCTSCheckersPlayer implements CheckersPlayer {
    private int numIterations;
    private MCTS<CheckersMove, CheckersGame, MCTSPlayer<CheckersMove, CheckersGame>> mcts;

    public MCTSCheckersPlayer(MCTSPlayer<CheckersMove, CheckersGame> player, int numIterations, double priorWeight, Random random) {
        this.numIterations = numIterations;
        this.mcts = new MCTS<CheckersMove, CheckersGame, MCTSPlayer<CheckersMove, CheckersGame>>(
                player, priorWeight, 0, random);
    }

    @Override
    public CheckersMove move(CheckersGame game) {
        mcts.resetRoot(); // Cannot reuse search trees through this interface because we don't know about the opponent's moves
        var result = mcts.search(game, numIterations);
        mcts.advanceToMove(result.chosenMove);
        return result.chosenMove;
    }

}
