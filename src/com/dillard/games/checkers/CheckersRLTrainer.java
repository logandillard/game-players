package com.dillard.games.checkers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.dillard.games.checkers.MCTS.MCTSResult;

public class CheckersRLTrainer {
    private static final int replayHistorySize = 128 * 1024; // TODO AlphaGoZero uses 500k
    private static final long NN_CHECKPOINT_INTERVAL_MS = 1000 * 60 * 5; // 5 minutes
    private double explorationFactor = 0.5; // TODO start at 1.0, anneal down to 0
    private Random random;
    private CheckersValueNN trainingNN;
    private List<TrainingExample> replayHistory = null;
    private PrioritizedSampler prioritizedSampler;
    private volatile boolean continueTraining = true;
    private long quittingTime = 0;
    private long lastProgressTimestamp = 0;
    private AtomicInteger gamesPlayedSinceLastProgress = new AtomicInteger(0);
    private Consumer<CheckersValueNN> nnCheckpointer;

    public CheckersRLTrainer(Random random, Consumer<CheckersValueNN> nnCheckpointer) {
        this.random = random;
        this.nnCheckpointer = nnCheckpointer;
        prioritizedSampler = new PrioritizedSampler(random);
    }

    public TrainingResult train(long durationMs, CheckersValueNN nn, List<TrainingExample> history)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        this.replayHistory = history;
        if (this.replayHistory == null) {
            this.replayHistory = new ArrayList<>();
        }
        prioritizedSampler.addAll(replayHistory);

        trainingNN = nn;
        if (trainingNN == null) {
            trainingNN = CheckersValueNN.build();
        }

        if (durationMs <= 0) {
            return new TrainingResult(trainingNN, replayHistory);
        }

        quittingTime = System.currentTimeMillis() + durationMs;

        // Start our game threads
        int numGameThreads = 3;
        List<Thread> gameThreads = new ArrayList<>();
        for (int i=0; i<numGameThreads; i++) {
            Thread t = new Thread(() -> {
               playGamesForTrainingData();
            });
            gameThreads.add(t);
            t.start();
        }

        // Start our training thread
        Thread trainingThread = new Thread(() -> {
            trainFromExamples();
        });
        trainingThread.start();

        // wait for our game threads to finish
        for (int i=0; i<numGameThreads; i++) {
            try {
                gameThreads.get(i).join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Stop our training thread
        continueTraining = false;
        try {
            trainingThread.join();
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }

        return new TrainingResult(trainingNN, replayHistory);
    }

    public static final class TrainingResult {
        public CheckersValueNN trainingNN;
        public List<TrainingExample> replayHistory = null;
        public TrainingResult(CheckersValueNN trainingNN, List<TrainingExample> replayHistory) {
            this.trainingNN = trainingNN;
            this.replayHistory = replayHistory;
        }
    }

    private void playGamesForTrainingData() {
        CheckersValueNN checkpointNN = trainingNN.cloneWeights();
        lastProgressTimestamp = System.currentTimeMillis();
        int gamesPlayedSinceCheckpointNN = 0;
        while (System.currentTimeMillis() < quittingTime) {

            GameResult result = playOneGameForTrainingData(checkpointNN);
//            System.out.println(String.format("%.0f (%.6f) Player1 start? %b",
//                    result.finalScore, result.error, result.startingPlayer1Turn));
            gamesPlayedSinceLastProgress.incrementAndGet();

            synchronized(this) { // so that this only runs once at a time
                replayHistory.addAll(result.newTrainingExamples);
                // Truncate replay history (global)
                if (replayHistory.size() > replayHistorySize) {
                    replayHistory = new ArrayList<>(replayHistory.subList(replayHistorySize / 10, replayHistory.size()));
                    prioritizedSampler = new PrioritizedSampler(random);
                    prioritizedSampler.addAll(replayHistory);
                } else {
                    prioritizedSampler.addAll(result.newTrainingExamples);
                }

                // Report progress (global)
                if (System.currentTimeMillis() - lastProgressTimestamp > 30000) {
                    long time = System.currentTimeMillis();
                    double gamesPerSecond = gamesPlayedSinceLastProgress.get() * 1000 / (double)(time - lastProgressTimestamp);
                    lastProgressTimestamp = time;
                    gamesPlayedSinceLastProgress.set(0);
                    System.out.println(String.format("%.2f games/sec. minutes remaining %.2f",
                            gamesPerSecond, (quittingTime - time) / 60000.0));
                }
            }

            // Clone the training NN
            // AlphaGoZero checkpoints every 1k training steps
            gamesPlayedSinceCheckpointNN++;
            if (gamesPlayedSinceCheckpointNN >= 20) {
                checkpointNN = trainingNN.cloneWeights();
                gamesPlayedSinceCheckpointNN = 0;
            }
        }
    }

    private void trainFromExamples() {
        // Wait until we have some training examples
        while (continueTraining && prioritizedSampler.getNodeCount() == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long lastCheckpointTime = System.currentTimeMillis();
        int miniBatchesTrained = 0;
        while (continueTraining) {
            // train
            trainMiniBatch(trainingNN, prioritizedSampler);
            miniBatchesTrained++;

            // Checkpoint the NN (save to disk) occasionally
            if (miniBatchesTrained % 100 == 0) {
                if (System.currentTimeMillis() - lastCheckpointTime > NN_CHECKPOINT_INTERVAL_MS) {
                    lastCheckpointTime = System.currentTimeMillis();
                    if (nnCheckpointer != null) {
                        try {
                            nnCheckpointer.accept(trainingNN);
                            System.out.println("Saved NN");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        System.out.println(String.format("Trained %d mini batches", miniBatchesTrained));
    }

//    final int maxOver = 10;
    final int miniBatchSize = 32;
    final double MAX_ERROR_VALUE = 1000;
    final int NUM_MCTS_ITERS = 100;
    final double MCTS_PRIOR_WEIGHT = 20.0; // higher leads to more exploration in MCTS
    private final double priorityExponent = 0.5;
    private final double importanceSamplingBiasExponent = 0.5; // anneal from 0.4 to 1.0

    private void trainMiniBatch(CheckersValueNN nn, PrioritizedSampler prioritizedSampler) {
        List<TrainingExample> miniBatch = new ArrayList<>();
        double totalPrioritySum = prioritizedSampler.getPrioritySum();
        int totalCount = prioritizedSampler.getNodeCount();
        double maxImportanceWeight = 0;
        for (int i=0; i<miniBatchSize; i++) {
            TrainingExample te = prioritizedSampler.sampleAndRemove();
            te.importanceWeight = Math.pow(totalPrioritySum / (te.priority * totalCount), importanceSamplingBiasExponent);
            miniBatch.add(te);
            if (te.importanceWeight > maxImportanceWeight) {
                maxImportanceWeight =  te.importanceWeight;
            }
        }
        // Scale weights by 1/maxImportanceWeight so that they only scale down for stability
        for (var te : miniBatch) {
            te.importanceWeight = te.importanceWeight / maxImportanceWeight;
        }


        // Poor man's prioritization
//        List<TrainingExample> miniBatch = new ArrayList<>();
//        for (int i=0; i<miniBatchSize; i++) {
//            int idx = random.nextInt(replayHistory.size());
//            var maxScoreExample = replayHistory.get(idx);
//            double maxScore = replayHistory.get(idx).priority;
//
//            for (int j=0; j<maxOver; j++) {
//                var te = replayHistory.get((idx + j) % replayHistory.size());
//                if (te.priority > maxScore) {
//                    maxScore = te.priority;
//                    maxScoreExample = te;
//                }
//            }
//            miniBatch.add(maxScoreExample);
//        }

        nn.trainMiniBatch(miniBatch);

        // re-score examples in miniBatch
        for (var te : miniBatch) {
            te.priority = Math.pow(Math.abs(nn.error(te)), priorityExponent);
        }
        prioritizedSampler.addAll(miniBatch);
    }

    private GameResult playOneGameForTrainingData(CheckersValueNN nn) {
        // Random starting player
        CheckersGame game = new CheckersGame(Math.random() < 0.5);
        boolean startingPlayer1Turn = game.isPlayer1Turn();
        NNCheckersPlayer player = new NNCheckersPlayer(nn);

        List<TrainingExample> trainingExamples = new ArrayList<>();
        double[] lastStateValues = new double[2];
        double sumError = 0;

        var mcts = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                player, MCTS_PRIOR_WEIGHT, explorationFactor, random);

        while (!game.isTerminated()) {
            // Evaluate state for loss
            StateEvaluation<CheckersMove> networkResult = player.evaluateState(game);

            int stateValueIdx = game.isPlayer1Turn() ? 0 : 1;
            sumError += Math.abs(lastStateValues[stateValueIdx] - networkResult.stateValue);
            lastStateValues[stateValueIdx] = networkResult.stateValue;

            // MCTS search
            // TODO is this a good idea?
            // if either player has < N pieces left, stop exploration
            if (game.getMinPlayerPieceCount() < 2) {
                mcts.setExplorationFactor(0);
            }
            MCTSResult<CheckersMove> result = mcts.search(game, NUM_MCTS_ITERS, true);

            // Store training example
            trainingExamples.add(new TrainingExample(
                    game.cloneBoard(),
                    game.isPlayer1Turn(),
                    0, // final game value - will update this later
                    result.scoredMoves,
                    MAX_ERROR_VALUE,
                    1.0 // importance weight
                    ));

            // take move
            game.move(result.chosenMove);

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
