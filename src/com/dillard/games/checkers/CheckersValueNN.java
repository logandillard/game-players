package com.dillard.games.checkers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.dillard.nn.ActivationFunction;
import com.dillard.nn.ActivationFunctionTanH;
import com.dillard.nn.LayeredNN;
import com.dillard.nn.WeightInitializer;
import com.dillard.nn.WeightInitializerGaussianFixedVariance;

public class CheckersValueNN implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int NUM_INPUTS = 128;
    private static final int NUM_OUTPUTS = 264;
    private LayeredNN nn;

    public CheckersValueNN(LayeredNN nn) {
        this.nn = nn;
    }

    public static CheckersValueNN build() {
        double learningRate = 0.001, l2 = 0.0001;
        ActivationFunction activation = new ActivationFunctionTanH();
        WeightInitializer initializer = new WeightInitializerGaussianFixedVariance(1.0/NUM_INPUTS);
        LayeredNN nn = LayeredNN.buildFullyConnected(new int[] {NUM_INPUTS, 100, NUM_OUTPUTS},
            activation,
            initializer,
            learningRate,
            l2
            );
        return new CheckersValueNN(nn);
    }

    @Override
    public CheckersValueNN clone() {
        return new CheckersValueNN(nn.clone());
    }

    public StateEvaluation<CheckersMove> evaluateState(CheckersGame game, List<CheckersMove> moves) {

        double[] inputs = createNNInputs(game.getBoardPieces(), !game.isPlayer1Turn());

        double[] output = nn.activate(inputs);

        double stateValue = output[0];
        List<Double> scores = new ArrayList<>(moves.size());

        // Include only indexes that are full legal moves
        for (CheckersMove move : moves) {
            int moveIndex = moveIndex(move);
            double score = output[1 + moveIndex]; // + 1 for the state value
            scores.add(score);
        }

        // Softmax to normalize scores to be probabilities
        // Softmax both the scores-by-move and the full move probs array
        softmaxInPlace(scores);

        return new StateEvaluation<>(stateValue, moves, scores);
    }

    private static final int KING_OFFSET = 64;
    private double[] createNNInputs(Piece[][] board, boolean mirrorForOpponent) {

        double[] inputs = new double[NUM_INPUTS];
        int idx = 0;
        for (int row=0; row<board.length; row++) {
            for (int col=row % 2; col<board[row].length; col += 2) {

                Piece p;
                if (!mirrorForOpponent) {
                    p = board[row][col];
                } else {
                    p = board[7 - row][7 - col];
                }

                if (p != null) {
                    int offset = 0;

                    // reverse color if we are building for the other player
                    boolean shouldOffset = p.getColor() == PieceColor.BLACK;
                    if (mirrorForOpponent) {
                        shouldOffset = !shouldOffset;
                    }
                    if (shouldOffset) {
                      offset = 32;
                    }

                    if (p.isKing()) {
                        inputs[idx + offset + KING_OFFSET] = 1.0;
                    } else {
                        inputs[idx + offset] = 1.0;
                    }
                }
                idx++;
            }
        }
        return inputs;
    }

    private int moveIndex(CheckersMove move) {
        // Find output for move
        var from = move.from;
        var to = move.to;
        // well, we have quite a few possible moves given that we have jumps to include
        int fromIndex = from.row * 4 + (from.col/2);
        int toDirectionOffset = (to.row > from.row ? 1 : 0) * 2 + to.col > from.col ? 1 : 0;
        int jumpOffset = move.jumpedLocation != null ? 0 : 4;
        int index = fromIndex * 8 + toDirectionOffset + jumpOffset;
        return index;
    }

    private void softmaxInPlace(List<Double> scores) {
        double sum = 0;
        for (double d : scores) {
            sum += Math.exp(d);
        }
        for (int i=0; i<scores.size(); i++) {
            scores.set(i, Math.exp(scores.get(i)) / sum);
        }
    }

    public double error(TrainingExample te) {
        double[] outputs = nn.activate(createNNInputs(te.state.getBoardPieces(), !te.isPlayer1));
        double[] correctOutputs = createCorrectOutputs(te, outputs);
        double error = 0.0;
        for (int i=0; i<outputs.length; i++) {
            double e = correctOutputs[i] - outputs[i];
            error += e * e;
        }
        return error;
    }

    public void trainMiniBatch(List<TrainingExample> miniBatch) {
        for (var te : miniBatch) {
            double[] outputs = nn.activate(createNNInputs(te.state.getBoardPieces(), !te.isPlayer1));
            double[] correctOutputs = createCorrectOutputs(te, outputs);
            double[] errorGradients = errorGradients(outputs, correctOutputs);

            nn.accumulateGradients(errorGradients);
        }
        nn.applyAccumulatedGradients();
    }

    private double[] errorGradients(double[] outputs, double[] correctOutputs) {
        double[] errorGradients = new double[outputs.length];
        for (int i=0; i<outputs.length; i++) {
            errorGradients[i] = correctOutputs[i] - outputs[i];
        }
        return errorGradients;
    }

    private double[] createCorrectOutputs(TrainingExample te, double[] actualOutputs) {
        // Retain the actual output for moves that were not legal possibilities.
        // We don't need the network to learn to output zero for illegal moves,
        // so we will give it zero error for illegal moves.
        double[] correctOutputs = actualOutputs.clone();

        correctOutputs[0] = te.finalGameValue; // location 0 is the state value
        for (Scored<CheckersMove> scoredMove : te.scoredMoves) {
            int idx = moveIndex(scoredMove.value);
            correctOutputs[idx + 1] = scoredMove.score;
        }
        return correctOutputs;
    }
}
