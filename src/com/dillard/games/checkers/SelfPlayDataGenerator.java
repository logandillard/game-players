package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.dillard.games.checkers.MCTS.MCTSResult;

public class SelfPlayDataGenerator {
    private Random random;
    private double explorationFactor = 1.0; // TODO start at 1.0, anneal down to 0
    final double MCTS_PRIOR_WEIGHT = 20.0; // higher values the priors more vs. the state values
    private int numMCTSIters = 200;
    final double MAX_ERROR_VALUE = 3;

    public SelfPlayDataGenerator(Random random) {
        this.random = random;
    }

    public GameResult playOneGameForTrainingData(CheckersValueNN nn) {
        // Random starting player
        CheckersGame game = new CheckersGame(Math.random() < 0.5);
        boolean startingPlayer1Turn = game.isPlayer1Turn();
        NNCheckersPlayer player = new NNCheckersPlayer(nn);

        List<TrainingExample> trainingExamples = new ArrayList<>();
//        boolean endedExploration = false;

        var mcts = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                player, MCTS_PRIOR_WEIGHT, explorationFactor, random);

        while (!game.isTerminated()) {
            // Evaluate state for loss
//            StateEvaluation<CheckersMove> networkResult = player.evaluateState(game);
//
//            int stateValueIdx = game.isPlayer1Turn() ? 0 : 1;
//            sumError += Math.abs(lastStateValues[stateValueIdx] - networkResult.stateValue);
//            lastStateValues[stateValueIdx] = networkResult.stateValue;

            // MCTS search
            // Stop exploration after 30 moves
//            if (!endedExploration && game.getMoveCount() >= 30) {
//                mcts.setExplorationFactor(0);
//                endedExploration = true;
//            }
            MCTSResult<CheckersMove> result = mcts.search(game, numMCTSIters, true);

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

        return new GameResult(trainingExamples, p1Score, startingPlayer1Turn);
    }

    public GameResult playOneGameForTrainingData(CheckersValueNN nn, CheckersValueNN opponentNN) {
        // Random starting player
        boolean playerIsPlayer1 = random.nextBoolean();
        CheckersGame game = new CheckersGame(true); // true = player1 goes first, but my player is not always P1
        NNCheckersPlayer player = new NNCheckersPlayer(nn);
        NNCheckersPlayer opponent = new NNCheckersPlayer(opponentNN);

        List<TrainingExample> trainingExamples = new ArrayList<>();
        var mcts = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                player, MCTS_PRIOR_WEIGHT, explorationFactor, random);
        var opponentMCTS = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                opponent, MCTS_PRIOR_WEIGHT, explorationFactor, random);

        while (!game.isTerminated()) {
            if (game.isPlayer1Turn() == playerIsPlayer1) {
                MCTSResult<CheckersMove> result = mcts.search(game, numMCTSIters, true);

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
                opponentMCTS.advanceToMove(result.chosenMove);
            } else {
                MCTSResult<CheckersMove> result = opponentMCTS.search(game, numMCTSIters, true);

                // do not store training example for opponent moves

                // take move
                game.move(result.chosenMove);

                // update gametree in MCTS
                mcts.advanceToMove(result.chosenMove);
                opponentMCTS.advanceToMove(result.chosenMove);
            }
        }

        double playerScore = game.getFinalScore(playerIsPlayer1);
        // upate all final game values
        for (var te : trainingExamples) {
            te.finalGameValue = playerScore;
        }

        return new GameResult(trainingExamples, playerScore, playerIsPlayer1);
    }

    public void setNumMCTSIters(int numMCTSIters) {
        this.numMCTSIters = numMCTSIters;
    }
}
