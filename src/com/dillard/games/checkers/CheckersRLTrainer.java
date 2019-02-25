package com.dillard.games.checkers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.dillard.games.checkers.MCTS.MCTSResult;

public class CheckersRLTrainer {
    private Random random;
    private static final int replayHistorySize = 64 * 1024;
    private double explorationFactor = 0.5; // TODO start at 1.0, anneal down to 0.000001
    private volatile boolean continueTraining = true;
    private long lastProgressTimestamp = 0;
    private AtomicInteger gamesPlayedSinceLastProgress = new AtomicInteger(0);
    private CheckersValueNN trainingNN;
    private List<TrainingExample> replayHistory = null;
    private String replayHistoryFilename;
    private String trainingNNFilename;
    private long quittingTime = 0;

    public CheckersRLTrainer(Random random, String replayHistoryFilename, String trainingNNFilename) {
        this.random = random;
        this.trainingNNFilename = trainingNNFilename;
        this.replayHistoryFilename = replayHistoryFilename;
    }

    public CheckersValueNN train(long durationMs) throws FileNotFoundException, IOException, ClassNotFoundException {
        loadOrDefaultNN();

        if (durationMs <= 0) {
            return trainingNN;
        }


        boolean loadReplayHistory = true;
        if (loadReplayHistory) {
            System.out.println("Loading replay history...");
            loadOrDefaultReplayHistory();
        } else {
            System.out.println("Not loading replay history.");
            replayHistory = new ArrayList<>();
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

//      final int toleranceIters = 4;
//      final double tolerance = 0.01;
//      double max = -1.0;
//      Random rand = new Random(23498);
//      List<Double> maxList = new ArrayList<>();
//        for (int i=0; i<maxIters; i++) {
//
//            // TODO multi-thread. need a different checkpoint NN for each thread because they hold state.
//
//            GameResult result = playOneGameForTrainingData(checkpointNN);
//            replayHistory.addAll(result.newTrainingExamples);
//            System.out.println(String.format("%.0f (%.6f) Player1 start? %b",
//                    result.finalScore, result.error, result.startingPlayer1Turn));
//
//            if (replayHistory.size() > replayHistorySize) {
//                replayHistory = replayHistory.subList(1024, replayHistory.size());
//            }
//
//            if (i % 10 == 0) {
//                checkpointNN = trainingNN.clone();
//            }
//
//            // TODO serialize network
//
//            // Convergence stuff
////            System.out.println(String.format("%02d  %.4f  - %s", i, result.accuracy, new Date()));
////
////            if (accuracy > max) {
////                max = accuracy;
////            }
////
////            maxList.add(max);
////
////            if (maxList.size() > toleranceIters) {
////                double ratio = (1.0 - max) / (1.0 - maxList.get(i - toleranceIters));
////                if (ratio > 1 - tolerance) {
////                    System.out.println("Converged");
////                    break;
////                }
////            }
//        }

//        System.out.println(String.format("Made %d training examples", replayHistory.size()));

        serializeNN(trainingNN, trainingNNFilename);
        System.out.println("Saved NN to " + trainingNNFilename);

        if (loadReplayHistory && !replayHistory.isEmpty()) {
            serializeReplayHistory(replayHistory, replayHistoryFilename);
            System.out.println(String.format("Saved replay history (%d) to ", replayHistory.size()) + replayHistoryFilename);
        }

        return trainingNN;
    }

    private void loadOrDefaultNN() throws FileNotFoundException, IOException, ClassNotFoundException {
        trainingNN = loadNN(trainingNNFilename);
        if (trainingNN == null) {
            trainingNN = CheckersValueNN.build();
        } else {
            System.out.println("Loaded neural network");
        }
    }

    private void loadOrDefaultReplayHistory() throws FileNotFoundException, IOException, ClassNotFoundException {
        replayHistory = loadReplayHistory(replayHistoryFilename);
        if (replayHistory == null) {
            replayHistory = new ArrayList<>(replayHistorySize);
        } else {
            System.out.println(String.format("Loaded replay history (%d)", replayHistory.size()));
        }
    }

    @SuppressWarnings("unchecked")
    private List<TrainingExample> loadReplayHistory(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (List<TrainingExample>) ois.readObject();
        } catch (FileNotFoundException fnf) {
            return null;
        }
    }

    private void serializeReplayHistory(List<TrainingExample> trainingExamples, String filename)
            throws FileNotFoundException, IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(trainingExamples);
        }
    }

    private void serializeNN(CheckersValueNN nn, String filename) throws FileNotFoundException, IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(nn);
        }
    }

    private CheckersValueNN loadNN(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (CheckersValueNN) ois.readObject();
        } catch (FileNotFoundException fnf) {
            return null;
        }
    }

    private void playGamesForTrainingData() {
        CheckersValueNN checkpointNN = trainingNN.clone();
        lastProgressTimestamp = System.currentTimeMillis();
        long lastCheckpointTime = System.currentTimeMillis();
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

            // Clone the training NN for this thread every 30s
            if (System.currentTimeMillis() - lastCheckpointTime > 30000) {
                checkpointNN = trainingNN.clone();
                lastCheckpointTime = System.currentTimeMillis();
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
        int miniBatchesTrained = 0;
        while (continueTraining) {
            trainMiniBatch(trainingNN, replayHistory);
            miniBatchesTrained++;

            if (miniBatchesTrained % 100 == 0) {
                if (System.currentTimeMillis() - lastCheckpointTime > 1000 * 60 * 5) {
                    lastCheckpointTime = System.currentTimeMillis();
                    try {
                        serializeNN(trainingNN, trainingNNFilename);
                        System.out.println("Saved NN to " + trainingNNFilename);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println(String.format("Trained %d mini batches", miniBatchesTrained));
    }

    final int maxOver = 10;
    final int miniBatchSize = 32;
    final double MAX_ERROR_VALUE = Double.MAX_VALUE;
    final int NUM_MCTS_ITERS = 100;
    final double MCTS_PRIOR_WEIGHT = 20.0; // higher leads to more exploration in MCTS

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
            MCTSResult<CheckersMove> result = mcts.search(game, NUM_MCTS_ITERS);

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
