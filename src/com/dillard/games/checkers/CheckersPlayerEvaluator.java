package com.dillard.games.checkers;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.dillard.games.checkers.MCTS.MCTSPlayer;
import com.dillard.games.checkers.MCTS.MCTSResult;

public class CheckersPlayerEvaluator {
    private double priorWeight;
    private int mctsIterations;
    private int opponentMCTSIterations;
    private boolean printGameSummaries = false;
    private boolean printMoves = false;
    private int nThreads = 4;

    public CheckersPlayerEvaluator(double priorWeight, int mctsIterations) {
        this.priorWeight = priorWeight;
        this.mctsIterations = mctsIterations;
    }

    public CheckersPlayerEvaluator(
            double priorWeight, int mctsIterations, int opponentMCTSIterations,
            int nThreads, boolean printGameSummaries, boolean printMoves) {
        this.priorWeight = priorWeight;
        this.mctsIterations = mctsIterations;
        this.opponentMCTSIterations = opponentMCTSIterations;
        this.printGameSummaries = printGameSummaries;
        this.printMoves = printMoves;
        this.nThreads = nThreads;
    }

    public EvaluationResult evaluate(
            MCTSPlayer<CheckersMove, CheckersGame> player,
            MCTSPlayer<CheckersMove, CheckersGame> opponent,
            int numGames, Random random) {
        EvaluationResult result = new EvaluationResult();

        ExecutorService es = Executors.newFixedThreadPool(nThreads);
        for (int i=0; i<numGames; i++) {
            es.submit(() -> {
                double score = playOneGameEvaluation(player, opponent, random);
                result.addResult(score);
            });
        }
        es.shutdown();
        try {
            es.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        for (int i=0; i<numGames; i++) {
//            double score = playOneGameEvaluation(player, opponent, random);
//            result.addResult(score);
//        }

        return result;
    }

    private double playOneGameEvaluation(
            MCTSPlayer<CheckersMove, CheckersGame> player,
            MCTSPlayer<CheckersMove, CheckersGame> opponent,
            Random random) {
        boolean player1Starts = Math.random() < 0.5;
        CheckersGame game = new CheckersGame(player1Starts);
        var mcts = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                player, priorWeight, 0.0, random);
        var opponentMCTS = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                opponent, priorWeight, 0.5, random);

        while (!game.isTerminated()) {
            if (printMoves) {
                System.out.println("P1? " + game.isPlayer1Turn());
            }
            if (game.isPlayer1Turn()) {
                // MCTS search
                MCTSResult<CheckersMove> result = mcts.search(game, mctsIterations, false);
                // take move
                game.move(result.chosenMove);
                // update gametree in MCTS
                mcts.advanceToMove(result.chosenMove);

                opponentMCTS.advanceToMove(result.chosenMove);
            } else {
                // MCTS search
                MCTSResult<CheckersMove> result = opponentMCTS.search(game, opponentMCTSIterations, false);
                // take move
                game.move(result.chosenMove);
                // update gametree in MCTS
                opponentMCTS.advanceToMove(result.chosenMove);
                mcts.advanceToMove(result.chosenMove);
            }

            if (printMoves) {
                System.out.println(game);
            }
        }

        double p1Score = game.getFinalScore(true);

        if (printGameSummaries) {
            System.out.println("P1 start? " + player1Starts);
            System.out.println(game);
            System.out.println(p1Score);
        }

        return p1Score;
    }

    public static final class EvaluationResult {
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
}
