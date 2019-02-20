package com.dillard.games.checkers;

import java.util.List;

import com.dillard.games.checkers.MCTS.MCTSMove;

public final class StateEvaluation<M extends MCTSMove> {
    public final double stateValue;
//    public final double[] fullMoveProbs;
    public final List<M> moves;
    public final List<Double> moveProbs;

    public StateEvaluation(double stateValue, // double[] fullMoveProbs,
            List<M> moves, List<Double> scores) {
        this.stateValue = stateValue;
        this.moves = moves;
        this.moveProbs = scores;
//        this.fullMoveProbs = fullMoveProbs;
    }

    @Override
    public String toString() {
        return String.format("%0.4f\n%s\n%s", stateValue, moves, moveProbs);
    }
}