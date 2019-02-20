package com.dillard.games.checkers;

import java.util.List;

import com.dillard.games.checkers.MCTS.ScoredMove;

public class TrainingExample {
    public final CheckersBoard state;
    public boolean isPlayer1;
    public double finalGameValue;
    public final List<ScoredMove<CheckersMove>> scoredMoves;
    public double priority;

    public TrainingExample(CheckersBoard state, boolean isPlayer1, double finalGameValue,
            List<ScoredMove<CheckersMove>> scoredMoves, double priority) {
        this.state = state;
        this.isPlayer1 = isPlayer1;
        this.finalGameValue = finalGameValue;
        this.scoredMoves = scoredMoves;
        this.priority = priority;
    }
}
