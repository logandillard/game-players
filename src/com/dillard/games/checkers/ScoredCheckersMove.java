package com.dillard.games.checkers;

public class ScoredCheckersMove {
    public final CheckersMove move;
    public final double score;

    public ScoredCheckersMove(CheckersMove move, double score) {
        this.move = move;
        this.score = score;
    }
}
