package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CheckersRLTrainer {
    private static final int replayHistorySize = 4 * 1024 * 1024; // TODO AlphaGoZero uses 500k games! That's about 40MM positions
    private static final long NN_CHECKPOINT_INTERVAL_MS = 1000 * 60 * 5; // 5 minutes
    private static final int INITIAL_TRAINING_MIN_POSITIONS = 128 * 1024;
    private Random random;
    private CheckersValueNN trainingNN;
    private ReplayHistory replayHistory;
    final int miniBatchSize = 32;
    private List<CheckersValueNN> oldNNs = new ArrayList<>();
    private final int MAX_NUM_OLD_NNS = 50;
    private int numGameThreads = 2;
    private volatile boolean continueTraining = true;
    private long quittingTime = 0;
    private long lastProgressTimestamp = 0;
    private AtomicInteger gamesPlayedSinceLastProgress = new AtomicInteger(0);
    private int totalGamesPlayed = 0;
    private int totalPositionsCreated = 0;
    private int miniBatchesTrained = 0;
    private Consumer<CheckersValueNN> nnCheckpointer;
    private Consumer<CheckersValueNN> nnEvaluator;
    private Supplier<Boolean> quitOverrider;
    private SelfPlayDataGenerator dataGenerator;

    public CheckersRLTrainer(
            Random random,
            Consumer<CheckersValueNN> nnCheckpointer,
            Consumer<CheckersValueNN> nnEvaluator,
            Supplier<Boolean> quitOverrider) {
        this.random = random;
        this.nnCheckpointer = nnCheckpointer;
        this.nnEvaluator = nnEvaluator;
        this.quitOverrider = quitOverrider;
        dataGenerator = new SelfPlayDataGenerator(random);
    }

    public TrainingResult train(long durationMs, CheckersValueNN nn, List<TrainingExample> history) {
        this.replayHistory = new ReplayHistory(random, replayHistorySize, history);

        trainingNN = nn;
        if (trainingNN == null) {
            trainingNN = CheckersValueNN.build();
        }

        quittingTime = System.currentTimeMillis() + durationMs;

//        // Initial produce initial training data
//        if (replayHistory.size() < INITIAL_TRAINING_MIN_POSITIONS) {
//            System.out.println(String.format("All threads producing training data until %d positions", INITIAL_TRAINING_MIN_POSITIONS));
//            List<Thread> initialGameThreads = new ArrayList<>();
//            for (int i=0; i<numGameThreads + 2; i++) {
//                Thread t = new Thread(() -> {
//                    playGamesForInitialTrainingData(INITIAL_TRAINING_MIN_POSITIONS);
//                });
//                initialGameThreads.add(t);
//                t.start();
//            }
//            // wait for our game threads to finish
//            for (int i=0; i<initialGameThreads.size(); i++) {
//                try {
//                    initialGameThreads.get(i).join();
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            System.out.println("Initial position requirement reached");
//        }

        // Start our game threads
        List<Thread> gameThreads = new ArrayList<>();
        for (int i=0; i<numGameThreads; i++) {
            Thread t = new Thread(() -> {
//               playGamesForTrainingDataSeparateOpponent();
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

        // Start our evaluation thread
        Thread evaluationThread = new Thread(() -> {
            while (continueTraining) {
                nnEvaluator.accept(trainingNN.cloneWeights());
//                evaluateAndUpdateBest();
            }
        });
        evaluationThread.start();

        // wait for quitting time
        try {
            while (System.currentTimeMillis() < quittingTime && !quitOverrider.get()) {
                Thread.sleep(Math.min(10000, quittingTime - System.currentTimeMillis()));
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        continueTraining = false;

        // wait for our game threads to finish
        for (int i=0; i<gameThreads.size(); i++) {
            try {
                gameThreads.get(i).join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // wait for our training thread to finish
        try {
            trainingThread.join();
            evaluationThread.join();
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }

        System.out.println(String.format("total games %d. training examples created %d",
                totalGamesPlayed, totalPositionsCreated));

        return new TrainingResult(trainingNN, replayHistory);
    }

//    final double MAX_ERROR_VALUE = 3;
//    final int NUM_MCTS_ITERS = 100;
//    final double MCTS_PRIOR_WEIGHT = 20.0; // higher values the priors more vs. the state values
//    private final double SCORE_THRESHOLD_FOR_BEST_NN_UPDATE = 0.3;
//    private void evaluateAndUpdateBest() {
//        CheckersValueNN candidateNN = trainingNN.clone();
//
//        CheckersPlayerEvaluator evaluator = new CheckersPlayerEvaluator(
//                MCTS_PRIOR_WEIGHT, NUM_MCTS_ITERS, NUM_MCTS_ITERS, 1, false, false);
//        EvaluationResult result = evaluator.evaluate(
//                new NNCheckersPlayer(candidateNN),
//                new NNCheckersPlayer(bestNN),
//                100,
//                new Random(432));
//
//        System.out.println(String.format(
//                "Evaluation vs previous best: %s",
//                result.toString()));
//
//        if (result.getScore() > SCORE_THRESHOLD_FOR_BEST_NN_UPDATE) {
//            System.out.println("Updating best nn");
//            if (nnCheckpointer != null) {
//                try {
//                    nnCheckpointer.accept(candidateNN);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            bestNN = candidateNN;
//        }
//    }

    public static final class TrainingResult {
        public CheckersValueNN trainingNN;
        public ReplayHistory replayHistory = null;
        public TrainingResult(CheckersValueNN trainingNN, ReplayHistory replayHistory) {
            this.trainingNN = trainingNN;
            this.replayHistory = replayHistory;
        }
    }

    private void playGamesForTrainingData() {
        long reportIntervalMs = 30000;
        CheckersValueNN playerNN = trainingNN.cloneWeights();
        lastProgressTimestamp = System.currentTimeMillis();
        int gamesPlayedSinceCheckpointNN = 0;
        while (System.currentTimeMillis() < quittingTime && continueTraining) {

            GameResult result = dataGenerator.playOneGameForTrainingData(playerNN);
//            System.out.println(String.format("%.0f (%.6f) Player1 start? %b",
//                    result.finalScore, result.error, result.startingPlayer1Turn));
            gamesPlayedSinceLastProgress.incrementAndGet();

            replayHistory.add(result.newTrainingExamples);
            synchronized(this) { // so that this only runs once at a time
                totalGamesPlayed++;
                totalPositionsCreated += result.newTrainingExamples.size();

                // Report progress (global)
                if (System.currentTimeMillis() - lastProgressTimestamp >= reportIntervalMs) {
                    long time = System.currentTimeMillis();
                    double gamesPerSecond = gamesPlayedSinceLastProgress.get() * 1000 / (double)(time - lastProgressTimestamp);
                    lastProgressTimestamp += reportIntervalMs;
                    gamesPlayedSinceLastProgress.set(0);
                    long msRemaining = quittingTime - lastProgressTimestamp;
                    System.out.println(String.format("%.2f games/sec. total %d (%d). trained %d. time remaining %s",
                            gamesPerSecond, totalGamesPlayed, totalPositionsCreated,
                            miniBatchesTrained * miniBatchSize,
                            String.format("%02d:%02d", msRemaining / 60000, (msRemaining % 60000) / 1000)));

                    if (quitOverrider.get()) {
                        continueTraining = false;
                    }
                }
            }

            // Clone the training NN
            // AlphaGoZero checkpoints every 1k training steps (4MM positions trained)
            // But a player plays 25k games for training data before updating to a new player
            gamesPlayedSinceCheckpointNN++;
            if (gamesPlayedSinceCheckpointNN >= 1000) {
                playerNN = trainingNN.cloneWeights();
                gamesPlayedSinceCheckpointNN = 0;
            }
        }
    }

    private void playGamesForInitialTrainingData(int thresholdPositions) {
        long reportIntervalMs = 30000;
        CheckersValueNN playerNN = trainingNN.cloneWeights();
        lastProgressTimestamp = System.currentTimeMillis();
        while (replayHistory.size() < thresholdPositions && continueTraining) {

            GameResult result = dataGenerator.playOneGameForTrainingData(playerNN);
//            System.out.println(String.format("%.0f (%.6f) Player1 start? %b",
//                    result.finalScore, result.error, result.startingPlayer1Turn));
            gamesPlayedSinceLastProgress.incrementAndGet();

            replayHistory.add(result.newTrainingExamples);
            synchronized(this) { // so that this only runs once at a time
                totalGamesPlayed++;
                totalPositionsCreated += result.newTrainingExamples.size();

                // Report progress (global)
                if (System.currentTimeMillis() - lastProgressTimestamp >= reportIntervalMs) {
                    long time = System.currentTimeMillis();
                    double gamesPerSecond = gamesPlayedSinceLastProgress.get() * 1000 / (double)(time - lastProgressTimestamp);
                    lastProgressTimestamp = time;
                    gamesPlayedSinceLastProgress.set(0);
                    long msRemaining = quittingTime - lastProgressTimestamp;
                    System.out.println(String.format("%.2f games/sec. total %d (%d). trained %d. time remaining %s",
                            gamesPerSecond, totalGamesPlayed, totalPositionsCreated,
                            miniBatchesTrained * miniBatchSize,
                            String.format("%02d:%02d", msRemaining / 60000, Math.round((msRemaining % 60000) / 1000.0))));

                    if (quitOverrider.get()) {
                        continueTraining = false;
                    }
                }
            }
        }
    }

    private void playGamesForTrainingDataSeparateOpponent() {
        CheckersValueNN playerNN = trainingNN.cloneWeights();
        CheckersValueNN opponentNN = null;
        lastProgressTimestamp = System.currentTimeMillis();
        int gamesPlayedSinceCheckpointNN = 0;

//        LinkedList<Double> recentScores = new LinkedList<>();
        EvaluationResult evaluationResult = new EvaluationResult();
//        final int MAX_RECENT_SCORES_SIZE = 100;
//        final double MIN_SCORE_FOR_OPPONENT_UPDATE = 0.3;
        while (System.currentTimeMillis() < quittingTime && continueTraining) {

            // choose opponent
            if (oldNNs.isEmpty()) {
                opponentNN = playerNN.cloneWeights();
            } else {
                opponentNN = oldNNs.get(random.nextInt(oldNNs.size()));
            }

            GameResult result = dataGenerator.playOneGameForTrainingData(playerNN, opponentNN);
            evaluationResult.addResult(result.finalScore);
            if (evaluationResult.numGames >= 50) {
                System.out.println(String.format("%s (%d)", evaluationResult.toString(), oldNNs.size()));
                evaluationResult = new EvaluationResult();
            }
//            recentScores.add(result.finalScore);
//            if (recentScores.size() > MAX_RECENT_SCORES_SIZE) {
//                double removedScore = recentScores.pop();
//                recentScoreSum -= removedScore;
//
//                double meanRecentScore = recentScoreSum / recentScores.size();
//                if (meanRecentScore >= MIN_SCORE_FOR_OPPONENT_UPDATE) {
//                    // TODO opponent update
//                    opponentNN = playerNN.cloneWeights();
//                    playerNN = trainingNN.cloneWeights();
//                    gamesPlayedSinceCheckpointNN = 0;
//                }
//            }
//            System.out.println(String.format("%.0f Player1 start? %b",
//                    result.finalScore, result.startingPlayer1Turn));
            gamesPlayedSinceLastProgress.incrementAndGet();

            replayHistory.add(result.newTrainingExamples);
            synchronized(this) { // so that this only runs once at a time
                totalGamesPlayed++;
                totalPositionsCreated += result.newTrainingExamples.size();
                // Report progress (global)
                if (System.currentTimeMillis() - lastProgressTimestamp > 30000) {
                    long time = System.currentTimeMillis();
                    double gamesPerSecond = gamesPlayedSinceLastProgress.get() * 1000 / (double)(time - lastProgressTimestamp);
                    lastProgressTimestamp = time;
                    gamesPlayedSinceLastProgress.set(0);
                    long msRemaining = quittingTime - time;
                    System.out.println(String.format("%.2f games/sec. total %d (%d). trained %d. minutes remaining %s",
                            gamesPerSecond, totalGamesPlayed, totalPositionsCreated,
                            miniBatchesTrained * miniBatchSize,
                            (msRemaining / 60000) + ":" + ((msRemaining % 60000) / 1000)));
                }
            }

            // Clone the training NN
            // AlphaGoZero checkpoints every 1k training steps (4MM positions trained)
            gamesPlayedSinceCheckpointNN++;
            if (gamesPlayedSinceCheckpointNN >= 50) {
                playerNN = trainingNN.cloneWeights();
                gamesPlayedSinceCheckpointNN = 0;
            }
        }
    }

    private void trainFromExamples() {
        // Wait until we have some training examples
        while (continueTraining && replayHistory.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long lastCheckpointTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < quittingTime && continueTraining) {
            // train
            trainMiniBatch(trainingNN);
            miniBatchesTrained++;

            // Checkpoint the NN (save to disk) occasionally
            if (miniBatchesTrained % 10 == 0 &&
                System.currentTimeMillis() - lastCheckpointTime > NN_CHECKPOINT_INTERVAL_MS) {
                lastCheckpointTime = System.currentTimeMillis();
                if (nnCheckpointer != null) {
                    try {
                        nnCheckpointer.accept(trainingNN);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (miniBatchesTrained % (10 * 1024 / miniBatchSize) == 0) {
                oldNNs.add(trainingNN.cloneWeights());
                if (oldNNs.size() > MAX_NUM_OLD_NNS) {
                    oldNNs = oldNNs.subList(oldNNs.size() - MAX_NUM_OLD_NNS, oldNNs.size());
                }
            }
        }
        System.out.println(String.format("Trained %d mini batches (%d positions)",
                miniBatchesTrained, miniBatchesTrained * miniBatchSize));
    }

    private void trainMiniBatch(CheckersValueNN nn) {
        List<TrainingExample> miniBatch = replayHistory.sample(miniBatchSize);
        nn.trainMiniBatch(miniBatch);
        replayHistory.reweightPriority(miniBatch, (TrainingExample te) -> nn.error(te));
    }

    public int getNumGameThreads() {
        return numGameThreads;
    }

    public void setNumGameThreads(int numGameThreads) {
        if (numGameThreads < 0) {
            throw new RuntimeException("Num game threads must be >= 0. Got " + numGameThreads);
        }
        this.numGameThreads = numGameThreads;
    }
}
