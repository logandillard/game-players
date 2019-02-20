package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.dillard.games.checkers.MCTS.SearchResult;

public class CheckersRLTrainer {
    private Random random;
    private static final int replayHistorySize = 16 * 1024;
    private double exploitationFactor = 0.5; // TODO anneal up to 1.0

    public CheckersRLTrainer(Random random) {
        this.random = random;
    }

    public CheckersValueNN train() {
        CheckersValueNN nn = CheckersValueNN.build();
        CheckersValueNN checkpointNN = nn.clone();

        final int maxIters = 50;
//        final int toleranceIters = 4;
//        final double tolerance = 0.01;
//        double max = -1.0;
//        Random rand = new Random(23498);
//        List<Double> maxList = new ArrayList<>();

        List<TrainingExample> replayHistory = new ArrayList<>(replayHistorySize);
        for (int i=0; i<maxIters; i++) {

            GameResult result = playOneGameForTrainingData(nn, checkpointNN);
            replayHistory.addAll(result.newTrainingExamples);
            System.out.println(String.format("%.0f (%.6f) Player1 start? %b",
                    result.finalScore, result.error, result.startingPlayer1Turn));

            // TODO should have a separate training thread!
            train(nn, replayHistory);

            if (i % 10 == 0) {
                checkpointNN = nn.clone();
            }

            if (replayHistory.size() > replayHistorySize) {
                replayHistory = replayHistory.subList(1024, replayHistory.size());
            }

            // TODO serialize network

            // Convergence stuff
//            System.out.println(String.format("%02d  %.4f  - %s", i, result.accuracy, new Date()));
//
//            if (accuracy > max) {
//                max = accuracy;
//            }
//
//            maxList.add(max);
//
//            if (maxList.size() > toleranceIters) {
//                double ratio = (1.0 - max) / (1.0 - maxList.get(i - toleranceIters));
//                if (ratio > 1 - tolerance) {
//                    System.out.println("Converged");
//                    break;
//                }
//            }
        }

        // just for debugging
        CheckersPlayer opponent = new RandomCheckersPlayer(new Random(9872345));
        double evaluation = evaluate(nn, opponent, 20);
        System.out.println(String.format("Evaluation: %f", evaluation));

        return nn;
    }

    private double evaluate(CheckersValueNN nn, CheckersPlayer opponent, int numGames) {
        double sum = 0;
        for (int i=0; i<numGames; i++) {
            sum += playOneGameEvaluation(nn, opponent);
        }
        return sum / numGames;
    }

    private double playOneGameEvaluation(CheckersValueNN nn, CheckersPlayer opponent) {
        CheckersGame game = new CheckersGame(Math.random() < 0.5);
        NNCheckersPlayer player = new NNCheckersPlayer(nn);
        var mcts = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(player, MCTS_PRIOR_WEIGHT, exploitationFactor, random);

        while (!game.isTerminated()) {
            if (game.isPlayer1Turn()) {
                // MCTS search
                mcts.resetRoot();
                SearchResult<CheckersMove> result = mcts.search(game, NUM_MCTS_ITERS);

                // take move
                game.move(result.chosenMove);

                // update gametree in MCTS
                mcts.advanceToMove(result.chosenMove);
            } else {
                CheckersMove move = opponent.move(game);
                game.move(move);
//                mcts.advanceToMove(move);
            }
        }

        double p1Score = game.getFinalScore(true);
        return p1Score;
    }

    final int maxOver = 10;
    final int miniBatchSize = 32;
    final int NUM_MINI_BATCHES_PER_TRAINING = 10;
    final double MAX_ERROR_VALUE = 1000.0;
    final int NUM_MCTS_ITERS = 100;
    final double MCTS_PRIOR_WEIGHT = 20.0; // higher leads to more exploration in MCTS

    private void train(CheckersValueNN nn, List<TrainingExample> replayHistory) {
        for (int i=0; i<NUM_MINI_BATCHES_PER_TRAINING; i++) {
            trainMiniBatch(nn, replayHistory);
        }
    }

    private void trainMiniBatch(CheckersValueNN nn, List<TrainingExample> replayHistory) {
        // TODO proper prioritization
        List<TrainingExample> miniBatch = new ArrayList<>();
        for (int i=0; i<miniBatchSize; i++) {
            int idx = random.nextInt(replayHistory.size());
            var maxScoreExample = replayHistory.get(idx);
            double maxScore = replayHistory.get(idx).priority;

            for (int j=0; j<maxOver; j++) {
                var te = replayHistory.get((idx + j) % replayHistory.size());
                if (te.priority > maxScore) {
                    maxScore = te.priority;
                    maxScoreExample = te;
                }
            }
            miniBatch.add(maxScoreExample);
        }

        nn.trainMiniBatch(miniBatch);

        // re-score examples in miniBatch
        for (var te : miniBatch) {
            te.priority = Math.abs(nn.error(te));
        }
    }

    private GameResult playOneGameForTrainingData(CheckersValueNN nn, CheckersValueNN checkpointNN) {
        // Random starting player
        CheckersGame game = new CheckersGame(Math.random() < 0.5);
        boolean startingPlayer1Turn = game.isPlayer1Turn();
        NNCheckersPlayer player = new NNCheckersPlayer(nn);

        List<TrainingExample> trainingExamples = new ArrayList<>();
        double[] lastStateValues = new double[2];
        double sumError = 0;

        var mcts = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                new PieceCountCheckersPlayer(), MCTS_PRIOR_WEIGHT, exploitationFactor, random);
//                player, MCTS_PRIOR_WEIGHT, exploitationFactor, random);

        while (!game.isTerminated()) {
            // Evaluate state for loss
            StateEvaluation<CheckersMove> networkResult = player.evaluateState(game);

            int stateValueIdx = game.isPlayer1Turn() ? 0 : 1;
            sumError += Math.abs(lastStateValues[stateValueIdx] - networkResult.stateValue);
            lastStateValues[stateValueIdx] = networkResult.stateValue;

            // MCTS search
            SearchResult<CheckersMove> result = mcts.search(game, NUM_MCTS_ITERS);

            // Store training example
            trainingExamples.add(new TrainingExample(
                    game.cloneBoard(),
                    game.isPlayer1Turn(),
                    0, // final game value - will update this later
                    result.scoredMoves,
                    MAX_ERROR_VALUE
                    ));

            // take move
            game.move(result.chosenMove);

            System.out.println(game);

            // update gametree in MCTS
            mcts.advanceToMove(result.chosenMove);
        }

        double p1Score = game.getFinalScore(true);
        // upate all final game values
        for (var te : trainingExamples) {
            te.finalGameValue = te.isPlayer1 ? p1Score : -p1Score;
        }

        return new GameResult(trainingExamples, sumError, p1Score, startingPlayer1Turn);
    }

    private static final class GameResult {
        List<TrainingExample> newTrainingExamples;
        double error;
        double finalScore;
        boolean startingPlayer1Turn;

        public GameResult(List<TrainingExample> newTrainingExamples, double error,
                double finalScore, boolean startingPlayer1Turn) {
            this.newTrainingExamples = newTrainingExamples;
            this.error = error;
            this.finalScore = finalScore;
            this.startingPlayer1Turn = startingPlayer1Turn;
        }
    }
}
