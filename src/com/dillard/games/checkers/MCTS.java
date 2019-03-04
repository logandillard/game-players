package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private double explorationFactor;
    private Random random;
    private static final double noiseEpsilon = 0.25;
    private static final double DIRICHLET_VALUE = 0.4;

    public MCTS(MCTSPlayer<M, G> player, double priorWeight, double explorationFactor, Random random) {
        this.player = player;
        this.priorWeight = priorWeight;
        this.explorationFactor = explorationFactor;
        this.random = random;
    }

    public MCTSResult<M> search(G game, int numIterations, boolean useDirichletNoise) {
        if (root == null) {
            root = new Node<M, G>(game, 1.0, null);
        }

        // Adding Dirichlet noise to the prior probabilities in the root node s0,
        // specifically P(s, a) = (1 − ep)p + ep*η, where η ∼ Dir(0.03) and ep = 0.25.
        // This noise ensures that all moves may be tried, but the search may still overrule bad moves
        if (useDirichletNoise) {
            expandNode(root);
            List<Node<M, G>> children = new ArrayList<>(root.children.values());
            double[] dirichletNoise = dirichletNoise(children.size());
            for (int i=0; i<children.size(); i++) {
                Node<M, G> child = children.get(i);
                child.priorProb = (1.0 - noiseEpsilon) * child.priorProb + noiseEpsilon * dirichletNoise[i];
            }
        }

        for (int i=0; i<numIterations; i++) {
            search(root);
        }

        return buildResult(root);
    }

    private double[] dirichletNoise(int n) {
        double[] p = new double[n];
        Arrays.fill(p, DIRICHLET_VALUE);
        Dirichlet dirichlet = new Dirichlet(p);
        return dirichlet.nextDistribution();
    }

    private double search(Node<M, G> node) {
        // expand node
        if (node.children.isEmpty()) {
            double stateValue = expandNode(node);
            // have hit what was a leaf, no need to continue
            return stateValue;
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
        var children = new ArrayList<>(root.children.entrySet());
        Collections.shuffle(children, random);

        int childVisitCountSum = 0;
        for (var entry : children) {
            Node<M, G> childNode = entry.getValue();
            childVisitCountSum += childNode.visitCount;
        }
        double sqrtChildVisitCountSum = Math.sqrt(1 + childVisitCountSum);

        for (var entry : children) {
            Node<M, G> childNode = entry.getValue();
            double opponentMultiplier = childNode.game.isPlayer1Turn() == root.game.isPlayer1Turn() ? 1.0 : -1.0;
            double observedValueScore = childNode.visitCount == 0 ? 0.0 :
                    opponentMultiplier * (childNode.observedValueSum / childNode.visitCount);
            double priorScore = priorWeight * childNode.priorProb * sqrtChildVisitCountSum / (1.0 + childNode.visitCount);
            double score = observedValueScore + priorScore;
            if (score > maxScore) {
                maxScore = score;
                maxScoreMove = entry.getKey();
            }
        }
//        For debugging
        if (maxScoreMove == null) {
            for (var entry : children) {
                Node<M, G> childNode = entry.getValue();
                double opponentMultiplier = childNode.game.isPlayer1Turn() == root.game.isPlayer1Turn() ? 1.0 : -1.0;
                double observedValueScore = childNode.visitCount == 0 ? 0.0 :
                    opponentMultiplier * (childNode.observedValueSum / childNode.visitCount);
                double priorScore = priorWeight * childNode.priorProb * sqrtChildVisitCountSum / (1.0 + childNode.visitCount);
                double score = observedValueScore + priorScore;
                System.out.println(String.format(
                        "observedValueScore: %f observedValueSum %f priorScore %f priorProb %f visitCount %d",
                        observedValueScore, childNode.observedValueSum, priorScore, childNode.priorProb, childNode.visitCount));
                if (score > maxScore) {
                    maxScore = score;
                    maxScoreMove = entry.getKey();
                }
            }
            throw new RuntimeException("No max score move!");
        }
        return maxScoreMove;
    }

    private double expandNode(Node<M, G> node) {
        if (node.game.isTerminated()) {
            // TODO supposed to set visit count to 0, observedValueSum to 0, and back up stateValue
            node.observedValueSum = node.game.getFinalScore(node.game.isPlayer1Turn());
            node.visitCount = 1;
            return node.observedValueSum;
        } else {
            var stateEvaluation = player.evaluateState(node.game);
            node.observedValueSum = 0;
            node.visitCount = 0;
            for (int i=0; i<stateEvaluation.moves.size(); i++) {
                var move = stateEvaluation.moves.get(i);
                var updatedGame = node.game.clone();
                updatedGame.move(move);
                node.children.put(move, new Node<M, G>(updatedGame, stateEvaluation.moveProbs.get(i), node));
            }
            return stateEvaluation.stateValue;
        }
    }

    public void advanceToMove(M move) {
        if (root != null) {
            root = root.children.get(move);
        }
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
            sb.append(String.format("%.3f (%d) %s %f\n%s\n",
                    observedValueSum / (1.0 + visitCount), visitCount,
                    game.isPlayer1Turn() ? "WHITE" : "BLACK", priorProb, game.toString()));
            for (Node<M, G> child : children.values()) {
                sb.append(String.format("%.3f (%d) %s\n",
                    child.visitCount == 0 ? 0.0 : child.observedValueSum / child.visitCount,
                    child.visitCount,
                    child.game.isPlayer1Turn() ? "WHITE" : "BLACK"));
            }
            return sb.toString();
        }
    }

    public static interface MCTSPlayer<M extends MCTSMove, G extends MCTSGame<M, G>> {
        StateEvaluation<M> evaluateState(G game);
        M move(G game);
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

    public MCTSResult<M> buildResult(Node<M, G> root) {
        List<Scored<M>> visitScoredMoves = new ArrayList<>();
        for (Map.Entry<M, Node<M, G>> entry : root.children.entrySet()) {
            visitScoredMoves.add(new Scored<M>(entry.getKey(), entry.getValue().visitCount));
        }

        // Handle the 0-exploration case
        if (explorationFactor <= 0) {
            // L_infinity normalize (return max)
            int maxIdx = 0;
            double maxScore = visitScoredMoves.get(0).score;
            List<Scored<M>> scoredMoves = new ArrayList<>();
            for (int i=0; i<visitScoredMoves.size(); i++) {
                double score = visitScoredMoves.get(i).score;
                scoredMoves.add(new Scored<M>(visitScoredMoves.get(i).value, 0));
                if (score > maxScore) {
                    maxScore = score;
                    maxIdx = i;
                }
            }
            scoredMoves.get(maxIdx).score = 1.0;
            return new MCTSResult<M>(scoredMoves, scoredMoves.get(maxIdx).value);
        }

        // select a move proportional to its exponentiated visit count, π(a|s0) = N(s0, a)^1/τ / SUM N(s0, b)^1/τ
        // where τ is a temperature parameter that controls the level of exploration
        List<Scored<M>> scoredMoves = new ArrayList<>();
        double exponent = 1.0 / explorationFactor;
        double sum = 0;
        for (int i=0; i<visitScoredMoves.size(); i++) {
            double score = Math.pow(visitScoredMoves.get(i).score, exponent);
            scoredMoves.add(new Scored<M>(visitScoredMoves.get(i).value, score));
            sum += score;
        }
        // Normalize move scores
        for (Scored<M> scoredMove : scoredMoves) {
            scoredMove.score /= sum;
        }

        // Sample
        double threshold = random.nextDouble();
        double accum = 0;
        M move = scoredMoves.get(scoredMoves.size() - 1).value;
        for (int i=0; i<scoredMoves.size(); i++) {
            accum += scoredMoves.get(i).score;
            if (accum >= threshold) {
                move = scoredMoves.get(i).value;
                break;
            }
        }

        return new MCTSResult<M>(scoredMoves, move);
    }

    public static final class MCTSResult<M extends MCTSMove> {
        public List<Scored<M>> scoredMoves;
        public M chosenMove;

        public MCTSResult(List<Scored<M>> scoredMoves, M chosenMove) {
            this.scoredMoves = scoredMoves;
            this.chosenMove = chosenMove;
        }

        @Override
        public String toString() {
            return chosenMove.toString() + " " + scoredMoves.toString();
        }
    }

    public void setExplorationFactor(double e) {
        this.explorationFactor = e;
    }
}
