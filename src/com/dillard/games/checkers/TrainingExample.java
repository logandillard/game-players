package com.dillard.games.checkers;

import java.io.Serializable;
import java.util.List;

public class TrainingExample implements Serializable {
    private static final long serialVersionUID = 1L;
    public final CheckersBoard state;
    public boolean isPlayer1;
    public double finalGameValue;
    public final List<Scored<CheckersMove>> scoredMoves;
    public double priority;

    public TrainingExample(CheckersBoard state, boolean isPlayer1, double finalGameValue,
            List<Scored<CheckersMove>> scoredMoves, double priority) {
        this.state = state;
        this.isPlayer1 = isPlayer1;
        this.finalGameValue = finalGameValue;
        this.scoredMoves = scoredMoves;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return (isPlayer1 ? "WHITE" : "BLACK") + "\n"
                + state.toString() + "\n"
                + finalGameValue + "\n"
                + scoredMoves.toString();
    }
}
