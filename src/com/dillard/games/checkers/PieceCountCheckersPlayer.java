package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.List;

import com.dillard.games.checkers.MCTS.MCTSPlayer;

public class PieceCountCheckersPlayer implements CheckersPlayer, MCTSPlayer<CheckersMove, CheckersGame> {

	public PieceCountCheckersPlayer() {
	}

    @Override
    public CheckersMove move(CheckersGame game) {
        // Should really be using MCTS anyway
//        return explainMove(game).move;
        throw new UnsupportedOperationException();
    }

    public StateEvaluation<CheckersMove> evaluateState(CheckersGame game) {
        double evaluation = game.evaluate(game.isPlayer1Turn());
        List<CheckersMove> moves = game.getMoves();
        List<Double> scores = new ArrayList<>();
        for (CheckersMove move : moves) {
            scores.add(1.0 / moves.size());
        }
        return new StateEvaluation<>(evaluation, moves, scores);
    }

//    private MoveWithScore maxScoreMove(List<Double> moveScores, List<CheckersMove> moves) {
//      double maxValue = -Double.MAX_VALUE;
//      CheckersMove maxValueMove = null;
//      for (int i=0; i<moveScores.size(); i++) {
//          double v = moveScores.get(i);
//          if (v > maxValue) {
//              maxValue = v;
//              maxValueMove = moves.get(i);
//          }
//      }
//      return new MoveWithScore(maxValueMove, maxValue);
//    }
//
//    public static final class MoveWithScore {
//        public MoveWithScore(CheckersMove m, double s) {
//            this.move = m;
//            this.score = s;
//        }
//        CheckersMove move;
//        double score;
//    }
}
