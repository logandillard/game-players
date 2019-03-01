package com.dillard.games.checkers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Random;

import com.dillard.games.checkers.CheckersPlayerEvaluator.EvaluationResult;
import com.dillard.games.checkers.CheckersRLTrainer.TrainingResult;

public class CheckersRLTrainerApp {

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        String replayHistoryFilename = "/Users/logan/game-players/replay_history.ser";
        String nnFilename = "/Users/logan/game-players/final_nn.ser";

        boolean doTraining = true;
        int trainingMinutes = 5;

        var nn = loadNN(nnFilename);

        if (doTraining) {
            boolean loadReplayHistory = true;
            List<TrainingExample> replayHistory = null;
            if (loadReplayHistory) {
                replayHistory = loadReplayHistory(replayHistoryFilename);
            }

            CheckersRLTrainer trainer = new CheckersRLTrainer(new Random(12345),
            (CheckersValueNN checkpointNN) ->  {
                try {
                    serializeNN(checkpointNN, nnFilename);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("Training");
            TrainingResult trainingResult = trainer.train(trainingMinutes * 60 * 1000, nn, replayHistory);
            nn = trainingResult.trainingNN;
            replayHistory = trainingResult.replayHistory;
            System.out.println("Finished training");

            serializeNN(nn, nnFilename);
            System.out.println("Saved NN to " + nnFilename);

            if (loadReplayHistory && !replayHistory.isEmpty()) {
                serializeReplayHistory(replayHistory, replayHistoryFilename);
                System.out.println(String.format("Saved replay history (%d) to ", replayHistory.size()) + replayHistoryFilename);
            }
        }

        // TODO
        // choose moves deterministically after 30 moves into the game?
        // anneal down the move exploration? anneal up the IS bias correction?
        // add evaluations at specific game states
        // error gradients are messed up because of tanh + softmax?
        // replay history is maybe too short

        System.out.println("Evaluating...");
        final double EVALUATOR_MCTS_PRIOR_WEIGHT = 20;
        final int opponentMCTSIterations = 256;
        CheckersPlayerEvaluator evaluator = new CheckersPlayerEvaluator(
                EVALUATOR_MCTS_PRIOR_WEIGHT, 128, opponentMCTSIterations, 4, true, false);
        EvaluationResult evalVsHeuristic = evaluator.evaluate(
                new NNCheckersPlayer(nn),
                new PieceCountCheckersPlayer(),
                400,
                new Random(432));
        System.out.println(String.format(
                "Evaluation vs HeuristicPlayer(%d): %s",
                opponentMCTSIterations,
                evalVsHeuristic.toString()));

        // TODO evaluate starting at some pre-determined game states, like end states
//        --------
//        |     W W|
//        |        |
//        |        |
//        |W       |
//        |        |
//        |        |
//        |     B  |
//        |        |
//         --------
//        --------
//        |        |
//        |    W   |
//        |        |
//        |        |
//        |   W    |
//        |        |
//        |     W  |
//        |  B     |
//         --------
//        --------
//        |        |
//        |    W   |
//        |       w|
//        |        |
//        |   W    |
//        |      B |
//        |        |
//        |  w     |
//         --------
//        --------
//        |     W  |
//        |        |
//        | W w   W|
//        |        |
//        |       w|
//        |        |
//        |   B    |
//        |        |
//         --------
//        --------
//        |     W  |
//        |B       |
//        |   W    |
//        |  w     |
//        |       w|
//        |        |
//        |       w|
//        |      w |
//         --------
    }


    @SuppressWarnings("unchecked")
    private static List<TrainingExample> loadReplayHistory(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            System.out.println("Loading replay history");
            return (List<TrainingExample>) ois.readObject();
        } catch (FileNotFoundException fnf) {
            System.out.println("No replay history found");
            return null;
        }
    }

    private static void serializeReplayHistory(List<TrainingExample> trainingExamples, String filename)
            throws FileNotFoundException, IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(trainingExamples);
        }
    }

    private static void serializeNN(CheckersValueNN nn, String filename) throws FileNotFoundException, IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(nn);
        }
    }

    private static CheckersValueNN loadNN(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            System.out.println("Loading NN");
            return (CheckersValueNN) ois.readObject();
        } catch (FileNotFoundException fnf) {
            return null;
        }
    }
}