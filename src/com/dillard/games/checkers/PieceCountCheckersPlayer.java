package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.List;

import com.dillard.games.checkers.MCTS.MCTSPlayer;

public class PieceCountCheckersPlayer implements CheckersPlayer, MCTSPlayer<CheckersMove, CheckersGame> {

	public PieceCountCheckersPlayer() {
	}

    @Override
    public CheckersMove move(CheckersGame game) {
        List<CheckersMove> moves = game.getMoves();
        double maxScore = -Double.MAX_VALUE;
        CheckersMove maxScoreMove = moves.get(0);
        for (CheckersMove move : moves) {
            CheckersGame updatedGame = game.clone();
            updatedGame.move(move);
            double evaluation = game.evaluatePieceCount(game.isPlayer1Turn());
            if (evaluation > maxScore) {
                maxScoreMove = move;
                maxScore = evaluation;
            }
        }
        return maxScoreMove;
    }

    public StateEvaluation<CheckersMove> evaluateState(CheckersGame game) {
        double evaluation = game.evaluatePieceCount(game.isPlayer1Turn()) / 10.0;
        List<CheckersMove> moves = game.getMoves();
        List<Double> scores = new ArrayList<>();
        for (@SuppressWarnings("unused") CheckersMove move : moves) {
            scores.add(1.0 / moves.size());
        }
        return new StateEvaluation<>(evaluation, moves, scores);
    }
}
