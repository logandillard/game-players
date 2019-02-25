package com.dillard.games.checkers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

public class CheckersRLTrainerApp {

    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        CheckersRLTrainer trainer = new CheckersRLTrainer(new Random(12345),
                "/Users/logan/game-players/replay_history.ser",
                "/Users/logan/game-players/final_nn.ser");
        System.out.println("Training");
        int trainingMinutes = 0;
        CheckersValueNN nn = trainer.train(trainingMinutes * 60 * 1000);
        System.out.println("Finished training");

        System.out.println("Evaluating...");
        final double EVALUATOR_MCTS_PRIOR_WEIGHT = 20;
        CheckersPlayerEvaluator evaluator = new CheckersPlayerEvaluator(EVALUATOR_MCTS_PRIOR_WEIGHT, 128, true, false);
//        double evalVsRandom = evaluator.evaluate(
//                new NNCheckersPlayer(nn),
//                new RandomCheckersPlayer(new Random(297350)),
//                100,
//                new Random(432));
//        System.out.println(String.format("Evaluation vs RandomPlayer: %f", evalVsRandom));

        double evalVsHeuristic = evaluator.evaluate(
                new NNCheckersPlayer(nn),
                new PieceCountCheckersPlayer(),
                100,
                new Random(432));
        System.out.println(String.format("Evaluation vs HeuristicPlayer: %f", evalVsHeuristic));
    }
}
