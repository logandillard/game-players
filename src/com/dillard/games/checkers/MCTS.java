package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.dillard.games.checkers.MCTS.MCTSGame;
import com.dillard.games.checkers.MCTS.MCTSMove;
import com.dillard.games.checkers.MCTS.MCTSPlayer;

/** Monte Carlo Tree Search */
public class MCTS<M extends MCTSMove, G extends MCTSGame<M, G>, P extends MCTSPlayer<M, G>> {
    private MCTSPlayer<M, G> player;
    private double priorWeight;
    private Node<M, G> root = null;
    private double exploitationFactor;
    private Random random;

    public MCTS(MCTSPlayer<M, G> player, double priorWeight, double exploitationFactor, Random random) {
        this.player = player;
        this.priorWeight = priorWeight;
        this.exploitationFactor = exploitationFactor;
        this.random = random;
    }

    public SearchResult<M> search(G game, int numIterations) {
        if (root == null) {
            root = new Node<M, G>(game, 1.0, null);
        }

        // TODO add dirichlet noise for initial root actions
        for (int i=0; i<numIterations; i++) {
            search(root);
        }

        return buildResult(root);
    }

    private double search(Node<M, G> node) {

        // expand node
        if (node.children.isEmpty()) {
            expandNode(node);

            // have hit what was a leaf, no need to continue
            return node.observedValueSum / node.visitCount;
        }

        // search recursively
        // select the move that that maximises an upper confidence bound
        // Q(s, a) + U(s, a),
        // where U(s, a) ∝ P(s, a)/(1 + N(s, a))
        MCTSMove maxScoreMove = findBestMove(node);
        Node<M, G> child = node.children.get(maxScoreMove);
        double leafValue = search(child);

        // Flip sign if the child's isPlayer1 is different from this
        if (child.game.isPlayer1Turn() != node.game.isPlayer1Turn()) {
            leafValue *= -1;
        }

        // increment visit count, update action value
        node.visitCount++;
        node.observedValueSum += leafValue;

        return leafValue;
    }

    private MCTSMove findBestMove(Node<M, G> root) {
        MCTSMove maxScoreMove = null;
        double maxScore = -Double.MAX_VALUE;
        for (var entry : root.children.entrySet()) {
            Node<M, G> childNode = entry.getValue();
            double opponentMultiplier = childNode.game.isPlayer1Turn() == root.game.isPlayer1Turn() ? 1.0 : -1.0;
            double observedValueScore = opponentMultiplier * (childNode.observedValueSum / (1.0 + childNode.visitCount));
            if (observedValueScore != 0.0) {
                @SuppressWarnings("unused")
                String s = "";
            }
            if (opponentMultiplier == 1.0) {
                @SuppressWarnings("unused")
                String s = "";
            }
            double priorScore = priorWeight * (childNode.priorProb / (1.0 + childNode.visitCount));
            double score = observedValueScore + priorScore;
            if (score > maxScore) {
                maxScore = score;
                maxScoreMove = entry.getKey();
            }
        }
        return maxScoreMove;
    }

    private void expandNode(Node<M, G> node) {
        if (node.game.isTerminated()) {
            node.observedValueSum = node.game.getFinalScore(node.game.isPlayer1Turn());
            node.visitCount = 1;
        } else {
            var stateEvaluation = player.evaluateState(node.game);
            node.observedValueSum = stateEvaluation.stateValue;
            node.visitCount = 1;
            for (int i=0; i<stateEvaluation.moves.size(); i++) {
                var move = stateEvaluation.moves.get(i);
                var updatedGame = node.game.clone();
                updatedGame.move(move);
                node.children.put(move, new Node<M, G>(updatedGame, stateEvaluation.moveProbs.get(i), node));
            }
        }
    }

    public void advanceToMove(CheckersMove move) {
        root = root.children.get(move);
    }

    public void resetRoot() {
        root = null;
    }

    public static final class Node<M extends MCTSMove, G extends MCTSGame<M, G>> {
        G game;
        int visitCount = 0;
        double observedValueSum = 0;
        double priorProb;
        Node<M, G> parent;
        Map<M, Node<M, G>> children = new HashMap<>();

        public Node(G game, double priorProb, Node<M, G> parent) {
            this.game = game;
            this.parent = parent;
            this.priorProb = priorProb;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%.3f (%d) %s\n%s\n",
                    observedValueSum / (1.0 + visitCount), visitCount,
                    game.isPlayer1Turn() ? "WHITE" : "BLACK", game.toString()));
            for (Node<M, G> child : children.values()) {
                sb.append(String.format("%.3f (%d) %s\n",
                    child.observedValueSum / (1.0 + child.visitCount), child.visitCount,
                    child.game.isPlayer1Turn() ? "WHITE" : "BLACK"));
            }
            return sb.toString();
        }
    }

    public static interface MCTSPlayer<M extends MCTSMove, G extends MCTSGame<M, G>> {
        StateEvaluation<M> evaluateState(G game);
    }

    public static interface MCTSGame<M extends MCTSMove, G extends MCTSGame<M, G>> {
        boolean isTerminated();
        double getFinalScore(boolean player1);
        boolean isPlayer1Turn();
        G clone();
        void move(M move);
    }

    public static interface MCTSMove {

    }

    public SearchResult<M> buildResult(Node<M, G> root) {
        List<ScoredMove<M>> visitScoredMoves = new ArrayList<>();
        for (Map.Entry<M, Node<M, G>> entry : root.children.entrySet()) {
            visitScoredMoves.add(new ScoredMove<M>(entry.getKey(), entry.getValue().visitCount));
        }

        // select a move proportional to its exponentiated visit count, π(a|s0) = N(s0, a)^1/τ / SUM N(s0, b)^1/τ
        // where τ is a temperature parameter that controls the level of exploration
        List<ScoredMove<M>> scoredMoves = new ArrayList<>();
        double exponent = 1.0 / exploitationFactor;
        double sum = 0;
        for (int i=0; i<visitScoredMoves.size(); i++) {
            double score = Math.pow(visitScoredMoves.get(i).score, exponent);
            scoredMoves.add(new ScoredMove<M>(visitScoredMoves.get(i).move, score));
            sum += score;
        }
        // Normalize move scores
        for (ScoredMove<M> scoredMove : scoredMoves) {
            scoredMove.score /= sum;
        }

        // Sample
        double threshold = random.nextDouble() * sum;
        double accum = 0;
        M move = scoredMoves.get(scoredMoves.size() - 1).move;
        for (int i=0; i<scoredMoves.size(); i++) {
            accum += scoredMoves.get(i).score;
            if (accum >= threshold) {
                move = scoredMoves.get(i).move;
                break;
            }
        }

        return new SearchResult<M>(scoredMoves, move);
    }

    public static final class ScoredMove<M extends MCTSMove> {
        public final M move;
        public double score;

        public ScoredMove(M move, double score) {
            this.move = move;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("%f  %s", score, move.toString());
        }
    }

    public static final class SearchResult<M extends MCTSMove> {
        public List<ScoredMove<M>> scoredMoves;
        public M chosenMove;

        public SearchResult(List<ScoredMove<M>> scoredMoves, M chosenMove) {
            this.scoredMoves = scoredMoves;
            this.chosenMove = chosenMove;
        }
    }
}
