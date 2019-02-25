package com.dillard.games.checkers;

import java.util.List;

import com.dillard.games.checkers.MCTS.MCTSPlayer;

public class NNCheckersPlayer implements CheckersPlayer, MCTSPlayer<CheckersMove, CheckersGame> {
    private CheckersValueNN nn;

	public NNCheckersPlayer(CheckersValueNN nn) {
	    this.nn = nn;
	}

    @Override
    public CheckersMove move(CheckersGame game) {
        // Should really be using MCTS anyway
//        return explainMove(game).move;
        throw new UnsupportedOperationException();
    }

    public StateEvaluation<CheckersMove> evaluateState(CheckersGame game) {
        List<CheckersMove> moves = game.getMoves();
        return nn.evaluateState(game, moves);
    }
}
