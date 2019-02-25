package com.dillard.games.checkers;

import java.util.Random;

import com.dillard.games.checkers.MCTS.MCTSPlayer;
import com.dillard.games.checkers.MCTS.MCTSResult;

public class CheckersPlayerEvaluator {
    private double priorWeight;
    private int mctsIterations;
    private boolean printGameSummaries = false;
    private boolean printMoves = false;

    public CheckersPlayerEvaluator(double priorWeight, int mctsIterations) {
        this.priorWeight = priorWeight;
        this.mctsIterations = mctsIterations;
    }

    public CheckersPlayerEvaluator(double priorWeight, int mctsIterations, boolean printGameSummaries, boolean printMoves) {
        this.priorWeight = priorWeight;
        this.mctsIterations = mctsIterations;
        this.printGameSummaries = printGameSummaries;
        this.printMoves = printMoves;
    }

    public double evaluate(MCTSPlayer<CheckersMove, CheckersGame> player,
            MCTSPlayer<CheckersMove, CheckersGame> opponent,
            int numGames, Random random) {
        double sum = 0;
        for (int i=0; i<numGames; i++) {
            sum += playOneGameEvaluation(player, opponent, random);
        }
        return sum / numGames;
    }

    private double playOneGameEvaluation(
            MCTSPlayer<CheckersMove, CheckersGame> player,
            MCTSPlayer<CheckersMove, CheckersGame> opponent,
            Random random) {
        boolean player1Starts = Math.random() < 0.5;
        CheckersGame game = new CheckersGame(player1Starts);
        var mcts = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                player, priorWeight, 0.5, random);
        var opponentMCTS = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                opponent, priorWeight, 0.5, random);

        while (!game.isTerminated()) {
//            if (game.cloneBoard().getNumBlackPieces() == 1) {
//                String s = "";
//            }
            if (printMoves) {
                System.out.println("P1? " + game.isPlayer1Turn());
            }
            if (game.isPlayer1Turn()) {
                // MCTS search
                MCTSResult<CheckersMove> result = mcts.search(game, mctsIterations);
                // take move
                game.move(result.chosenMove);
                // update gametree in MCTS
                mcts.advanceToMove(result.chosenMove);

                opponentMCTS.advanceToMove(result.chosenMove);
            } else {
                // MCTS search
                MCTSResult<CheckersMove> result = opponentMCTS.search(game, mctsIterations);
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
}
