package com.dillard.games.checkers;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.dillard.games.ABPruningPlayer;
import com.dillard.games.GamePlayer;
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
            Supplier<GamePlayer<CheckersMove, CheckersGame>> opponentFactory,
            int numGames, Random random) {
        EvaluationResult result = new EvaluationResult();

        ExecutorService es = Executors.newFixedThreadPool(nThreads);
        for (int i=0; i<numGames; i++) {
            es.submit(() -> {
                try {
                    double score = playOneGameEvaluation(player, opponentFactory, random);
                    result.addResult(score);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        es.shutdown();
        try {
            es.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        for (int i=0; i<numGames; i++) {
//            double score = playOneGameEvaluation(player, opponentFactory, random);
//            result.addResult(score);
//        }

        return result;
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
            es.awaitTermination(30, TimeUnit.MINUTES);
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
        boolean player1Starts = random.nextBoolean();
        CheckersGame game = new CheckersGame(player1Starts);
        var mcts = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                player, priorWeight, 0.0, random);
        var opponentMCTS = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                opponent, priorWeight, 0.0, random);

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

    private double playOneGameEvaluation(
            MCTSPlayer<CheckersMove, CheckersGame> player,
            Supplier<GamePlayer<CheckersMove, CheckersGame>> opponentFactory,
            Random random) {
        boolean player1Starts = random.nextBoolean();
        CheckersGame game = new CheckersGame(player1Starts);
        var mcts = new MCTS<CheckersMove, CheckersGame, NNCheckersPlayer>(
                player, priorWeight, 0.0, random);
        GamePlayer<CheckersMove, CheckersGame> opponent = opponentFactory.get();

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

                if (opponent instanceof ABPruningPlayer<?, ?>) {
                    ((ABPruningPlayer<CheckersMove, CheckersGame>)opponent).advanceToMove(result.chosenMove);
                }
            } else {
                CheckersMove move = opponent.move(game);
                game.move(move);
                mcts.advanceToMove(move);
                if (opponent instanceof ABPruningPlayer<?, ?>) {
                    ((ABPruningPlayer<CheckersMove, CheckersGame>)opponent).advanceToMove(move);
                }
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
