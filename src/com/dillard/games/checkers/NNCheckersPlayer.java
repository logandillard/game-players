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
