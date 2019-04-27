package com.dillard.games.checkers;

public final class EvaluationResult {
    public double scoreSum;
    public int numWins;
    public int numLosses;
    public int numDraws;
    public int numGames;

    public synchronized void addResult(double score) {
        numGames++;
        scoreSum += score;
        if (score == 0.0) {
            numDraws++;
        } else if (score < 0) {
            numLosses++;
        } else {
            numWins++;
        }
    }

    public double getScore() {
        return scoreSum / numGames;
    }

    @Override
    public String toString() {
        return String.format("score: %.2f wins: %d losses: %d draws: %d", getScore(), numWins, numLosses, numDraws);
    }
}