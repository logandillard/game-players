package com.dillard.games.checkers;

import java.util.List;

final class GameResult {
    List<TrainingExample> newTrainingExamples;
    double finalScore;
    boolean startingPlayer1Turn;

    public GameResult(List<TrainingExample> newTrainingExamples,
            double finalScore, boolean startingPlayer1Turn) {
        this.newTrainingExamples = newTrainingExamples;
        this.finalScore = finalScore;
        this.startingPlayer1Turn = startingPlayer1Turn;
    }
}