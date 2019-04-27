package com.dillard.games.checkers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.dillard.nn.ActivationFunctionLinear;
import com.dillard.nn.ActivationFunctionReLU;
import com.dillard.nn.ActivationFunctionTanH;
import com.dillard.nn.LayeredNN;
import com.dillard.nn.NNLayer;
import com.dillard.nn.NNLayerFullyConnected;
import com.dillard.nn.NNLayerResidual;
import com.dillard.nn.WeightInitializer;
import com.dillard.nn.WeightInitializerGaussianFixedVariance;

public class CheckersValueNN implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int NUM_INPUTS = 128;
    private static final int NUM_OUTPUTS = 264;
    private ActivationFunctionTanH tanh = new ActivationFunctionTanH();
    private LayeredNN nn;

    public CheckersValueNN(LayeredNN nn) {
        this.nn = nn;
    }

    public static CheckersValueNN build() {
        double learningRate = 0.0001, l2 = 0.0001;
        WeightInitializer initializer = new WeightInitializerGaussianFixedVariance(1.0/NUM_INPUTS);

        int nHidden = 32;
        NNLayer[] layers = new NNLayer[] {
            new NNLayerFullyConnected(NUM_INPUTS, nHidden, new ActivationFunctionReLU(), initializer, learningRate, l2),
            new NNLayerResidual(new NNLayer[] {
                    new NNLayerFullyConnected(nHidden, nHidden, new ActivationFunctionReLU(), initializer, learningRate, l2),
                    new NNLayerFullyConnected(nHidden, nHidden, new ActivationFunctionReLU(), initializer, learningRate, l2),
            }),
            new NNLayerResidual(new NNLayer[] {
                    new NNLayerFullyConnected(nHidden, nHidden, new ActivationFunctionReLU(), initializer, learningRate, l2),
                    new NNLayerFullyConnected(nHidden, nHidden, new ActivationFunctionReLU(), initializer, learningRate, l2),
            }),
            new NNLayerResidual(new NNLayer[] {
                    new NNLayerFullyConnected(nHidden, nHidden, new ActivationFunctionReLU(), initializer, learningRate, l2),
                    new NNLayerFullyConnected(nHidden, nHidden, new ActivationFunctionReLU(), initializer, learningRate, l2),
            }),
            new NNLayerFullyConnected(nHidden, NUM_OUTPUTS, new ActivationFunctionLinear(), initializer, learningRate, l2)
        };
        LayeredNN nn =  new LayeredNN(layers);
        return new CheckersValueNN(nn);
    }

    @Override
    public CheckersValueNN clone() {
        return new CheckersValueNN(nn.clone());
    }

    /**
     * Does not clone fields related to ongoing learning, just weights for making predictions.
     */
    public CheckersValueNN cloneWeights() {
        return new CheckersValueNN(nn.cloneWeights());
    }

    public StateEvaluation<CheckersMove> evaluateState(CheckersGame game, List<CheckersMove> moves) {
        double[] inputs = createNNInputs(game.getBoardPieces(), !game.isPlayer1Turn());
        double[] output = nn.activate(inputs);

        double stateValue = tanh.activate(output[0]);
        List<Double> scores = new ArrayList<>(moves.size());

        // Include only indexes that are legal moves
        for (CheckersMove move : moves) {
            int moveIndex = moveIndex(move);
            double score = output[1 + moveIndex]; // + 1 for the state value
            scores.add(score);
        }
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
        // stabilize to prevent overflow or underflow
        double max = scores.get(0);
        for (double score : scores) {
            if (score > max) {
                max = score;
            }
        }
        double sum = 0;
        for (int i=0; i<scores.size(); i++) {
            double exp = Math.exp(scores.get(i) - max);
            sum += exp;
            scores.set(i, exp);
        }
        for (int i=0; i<scores.size(); i++) {
            scores.set(i, scores.get(i) / sum);
        }
    }

    public double error(TrainingExample te) {
        double[] outputs = nn.activate(createNNInputs(te.state.getBoardPieces(), !te.isPlayer1));
        double stateValue = tanh.activate(outputs[0]);
        double stateError = te.finalGameValue - stateValue;
        double error = stateError * stateError;

        if (te.scoredMoves.size() > 1) {
            List<Double> outputScores = new ArrayList<>(te.scoredMoves.size());
            for (int i=0; i<te.scoredMoves.size(); i++) {
                var scoredMove = te.scoredMoves.get(i);
                int moveIdx = moveIndex(scoredMove.value);
                int index = moveIdx + 1; // + 1 for the state value
                double rawOutput = outputs[index];
                outputScores.add(rawOutput);
            }

            softmaxInPlace(outputScores);

            for (int i=0; i<te.scoredMoves.size(); i++) {
                var scoredMove = te.scoredMoves.get(i);
                double softmaxOutput = outputScores.get(i);

//                double e = (scoredMove.score - softmaxOutput);
//                error += e*e;
                double e = -scoredMove.score * Math.log(softmaxOutput);
                error += e;
            }
        }
        if (Double.isNaN(error)) {
            throw new RuntimeException("Error is NaN!");
        }
        return error;
    }

    public double trainMiniBatch(List<TrainingExample> miniBatch) {

//        double errorSumBefore = 0;
//        for (var te : miniBatch) {
//            errorSumBefore += error(te);
//        }
        double errorSum = 0;

        for (var te : miniBatch) {
            double instanceError = 0;
            double[] outputs = nn.activate(createNNInputs(te.state.getBoardPieces(), !te.isPlayer1));
            double stateValue = tanh.activate(outputs[0]);

            // We don't need the network to learn to output zero for illegal moves,
            // so we will give it zero error for illegal moves.
            double[] errorGradients = new double[outputs.length];
            // location 0 is the state value
            errorGradients[0] = (te.finalGameValue - stateValue) * tanh.derivative(stateValue) * te.importanceWeight;
            instanceError += (te.finalGameValue - stateValue) * (te.finalGameValue - stateValue);

            List<Double> outputScores = new ArrayList<>(te.scoredMoves.size());
            if (te.scoredMoves.size() > 1) {
                List<Integer> indexes = new ArrayList<>(te.scoredMoves.size());
                for (int i=0; i<te.scoredMoves.size(); i++) {
                    var scoredMove = te.scoredMoves.get(i);
                    int moveIdx = moveIndex(scoredMove.value);
                    int index = moveIdx + 1; // + 1 for the state value
                    indexes.add(index);

                    double rawOutput = outputs[index];
                    outputScores.add(rawOutput);
                }

                softmaxInPlace(outputScores);

                for (int i=0; i<te.scoredMoves.size(); i++) {
                    var scoredMove = te.scoredMoves.get(i);
                    int index = indexes.get(i);
                    double softmaxOutput = outputScores.get(i);

                    instanceError += -scoredMove.score * Math.log(softmaxOutput);

                    if ((outputs[index] <= -100 && scoredMove.score < softmaxOutput) ||
                            (outputs[index] >= 100.0 && scoredMove.score > softmaxOutput)) {
                        // why you gotta keep pushing???? TODO is there a way to prevent this?
//                        System.out.println("pushing score down really low or up really high: " + outputs[index]);
                        // we will skip. the score is extreme enough!
                        continue;
                    }

                    errorGradients[index] = (scoredMove.score - softmaxOutput) * te.importanceWeight;
                }
            }
            nn.accumulateGradients(errorGradients);
//            nn.backprop(errorGradients);

//            if (instanceError > 10) {
//                String breakpoint= "";
//            }

            errorSum += instanceError;

//            if (true) {
//                outputs = nn.activate(createNNInputs(te.state.getBoardPieces(), !te.isPlayer1));
//                double stateValueAfter = tanh.activate(outputs[0]);
//
//                List<Double> scores = new ArrayList<>();
//                for (Scored<CheckersMove> sm : te.scoredMoves) {
//                    int moveIndex = moveIndex(sm.value);
//                    double score = outputs[1 + moveIndex]; // + 1 for the state value
//                    scores.add(score);
//                }
//                softmaxInPlace(scores);
//
//                if (true) {
//                    @SuppressWarnings("unused")
//                    String s = "";
//                }
//            }

        }
        nn.applyAccumulatedGradients();

//        double errorSumAfter = 0;
//        for (var te : miniBatch) {
//            errorSumAfter += error(te);
//        }
//        System.out.println(String.format("%.3f -> %.3f (%f)",
//                errorSumBefore/miniBatch.size(),
//                errorSumAfter/miniBatch.size(),
//                errorSumAfter/errorSumBefore));

//        System.out.println(String.format("%.3f", errorSum / miniBatch.size()));
        return errorSum;
    }

    public LayeredNN getNN() {
        return this.nn;
    }
}
