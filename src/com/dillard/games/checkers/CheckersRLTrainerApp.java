package com.dillard.games.checkers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Random;

import com.dillard.games.ABPruningPlayer;
import com.dillard.games.checkers.CheckersPlayerEvaluator.EvaluationResult;
import com.dillard.games.checkers.CheckersRLTrainer.TrainingResult;
import com.dillard.nn.LayeredNN;

public class CheckersRLTrainerApp {

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        String replayHistoryFilename = "/Users/logan/game-players/replay_history.ser";
        String nnFilename = "/Users/logan/game-players/final_nn.ser";

        boolean doTraining = true;
        boolean loadReplayHistory = true;
        boolean saveNN = true;
        int numTrainTestIterations = 1;
        int trainingMinutesPerIteration = 60;

        // TODO
        // choose moves deterministically after 30 moves into the game?
        // anneal down the move exploration? anneal up the IS bias correction?
        // add evaluations at specific game states
        // replay history is maybe too short

        CheckersValueNN nn = loadOrDefaultNN(nnFilename);
        nn.getNN().setLearningRate(0.0001);
        if (!doTraining) {
            System.out.println("Evaluating...");
            evaluate(nn, 100, 4);
            return;
        }

        List<TrainingExample> replayHistory = null;
        if (loadReplayHistory) {
            replayHistory = loadReplayHistory(replayHistoryFilename);
        }

        for (int iter=0; iter<numTrainTestIterations; iter++) {
            nn = trainOneIteration(replayHistoryFilename, nnFilename, loadReplayHistory, saveNN,
                    trainingMinutesPerIteration, nn, replayHistory);
            System.out.println(String.format("Trained total %d minutes", (iter+1) * trainingMinutesPerIteration));
            System.out.println("Evaluating...");
            evaluate(nn, 100, 4);
        }
    }

    private static CheckersValueNN trainOneIteration(String replayHistoryFilename, String nnFilename,
            boolean saveReplayHistory, boolean saveNN, int trainingMinutes,
            CheckersValueNN nn, List<TrainingExample> replayHistory)
            throws FileNotFoundException, IOException, ClassNotFoundException {

        System.out.println("Training");
        CheckersRLTrainer trainer = new CheckersRLTrainer(
            new Random(12345),
            (CheckersValueNN checkpointNN) ->  {
                if (saveNN) {
                    try {
                        serializeNN(checkpointNN.getNN(), nnFilename);
                        System.out.println("Saved NN");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            },
            (CheckersValueNN checkpointNN) ->  { evaluate(checkpointNN, 100, 1); }
            );

        // slow down game generation since it is now much faster than training.
        // this allows us to train repeatedly on each position, prioritizing those with higher error...
        // though I don't know if this actually makes a difference or not.
        trainer.setNumGameThreads(3);

        TrainingResult trainingResult = trainer.train(trainingMinutes * 60 * 1000, nn, replayHistory);
        nn = trainingResult.trainingNN;
        replayHistory = trainingResult.replayHistory;
        System.out.println("Finished training");

        if (saveNN) {
            serializeNN(nn.getNN(), nnFilename);
            System.out.println("Saved NN to " + nnFilename);
        }

        if (saveReplayHistory && !replayHistory.isEmpty()) {
            serializeReplayHistory(replayHistory, replayHistoryFilename);
        }
        return nn;
    }


    private static void evaluate(CheckersValueNN nn, final int ngames, final int nThreads) {
        final double EVALUATOR_MCTS_PRIOR_WEIGHT = 20;
        final int mctsIterations = 400;
        final int opponentMCTSIterations = 256;
        final int abPruningDepth = 6;
        CheckersPlayerEvaluator evaluator = new CheckersPlayerEvaluator(
                EVALUATOR_MCTS_PRIOR_WEIGHT, mctsIterations, opponentMCTSIterations, nThreads, false, false);
        EvaluationResult evalVsHeuristic = evaluator.evaluate(
                new NNCheckersPlayer(nn),
                () -> new ABPruningPlayer<CheckersMove, CheckersGame>(abPruningDepth, new Random(7349)),
                ngames,
                new Random(432));
        System.out.println(String.format(
                "Evaluation vs ABPruningPlayer(%d): %s",
                abPruningDepth,
                evalVsHeuristic.toString()));

//        EvaluationResult evalVsHeuristic = evaluator.evaluate(
//                new NNCheckersPlayer(nn),
//                new PieceCountCheckersPlayer(),
//                ngames,
//                new Random(432));
//        System.out.println(String.format(
//                "Evaluation vs PieceCountingPlayer(%d): %s",
//                opponentMCTSIterations,
//                evalVsHeuristic.toString()));
    }
    // TODO evaluate starting at some pre-determined game states, like end states
//  --------
//  |     W W|
//  |        |
//  |        |
//  |W       |
//  |        |
//  |        |
//  |     B  |
//  |        |
//   --------
//  --------
//  |        |
//  |    W   |
//  |        |
//  |        |
//  |   W    |
//  |        |
//  |     W  |
//  |  B     |
//   --------
//  --------
//  |        |
//  |    W   |
//  |       w|
//  |        |
//  |   W    |
//  |      B |
//  |        |
//  |  w     |
//   --------
//  --------
//  |     W  |
//  |        |
//  | W w   W|
//  |        |
//  |       w|
//  |        |
//  |   B    |
//  |        |
//   --------
//  --------
//  |     W  |
//  |B       |
//  |   W    |
//  |  w     |
//  |       w|
//  |        |
//  |       w|
//  |      w |
//   --------

    private static CheckersValueNN loadOrDefaultNN(String nnFilename)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        var rawNN = loadNN(nnFilename);
        if (rawNN != null) {
            return new CheckersValueNN(rawNN);
        } else {
            System.out.println("No serialized neural network found. Starting from scratch");
            return CheckersValueNN.build();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<TrainingExample> loadReplayHistory(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename), 8*1024*1024))) {
            System.out.println("Loading replay history");
            return (List<TrainingExample>) ois.readObject();
        } catch (FileNotFoundException fnf) {
            System.out.println("No replay history found");
            return null;
        }
    }

    private static void serializeReplayHistory(List<TrainingExample> trainingExamples, String filename)
            throws FileNotFoundException, IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename), 8*1024*1024))) {
            System.out.println("Saving replay history (do not cancel!)");
            oos.writeObject(trainingExamples);
            System.out.println(String.format("Saved replay history (%d) to ", trainingExamples.size()) + filename);
        }
    }

    private static void serializeNN(LayeredNN nn, String filename) throws FileNotFoundException, IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename), 8*1024*1024))) {
            oos.writeObject(nn);
        }
    }

    private static LayeredNN loadNN(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename), 8*1024*1024))) {
            System.out.println("Loading NN");
            return (LayeredNN) ois.readObject();
        } catch (FileNotFoundException fnf) {
            return null;
        }
    }
}
