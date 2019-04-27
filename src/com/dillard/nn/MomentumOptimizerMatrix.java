package com.dillard.nn;

import java.io.Serializable;

/**
 * Momentum optimizer for a neural network weight matrix. Does SGD with momentum updates. Not aware of biases.
 */
public final class MomentumOptimizerMatrix implements Serializable, OptimizerMatrix {
    private static final long serialVersionUID = 1L;
    private final double[][] momentumMatrix;
    private double momentum = 0.9;
    private double learningRate;
    private double lrTimesL2; // precomputed for speed

    public MomentumOptimizerMatrix(int weightRows, int weightCols,
            double learningRate, double l2Regularization, double momentum) {
        this.learningRate = learningRate;
        this.lrTimesL2 = learningRate * l2Regularization;
        this.momentum = momentum;
        momentumMatrix = new double[weightRows][weightCols];
    }

    /** Not a clone, just a new one with the same parameters. Does not copy state. */
    public OptimizerMatrix copyNew() {
        return new MomentumOptimizerMatrix(
                momentumMatrix.length, momentumMatrix[0].length, learningRate, lrTimesL2 / learningRate, momentum);
    }

    public final void update(final double[][] weights, int row, int col, double gradient, final boolean applyRegularization) {
        double update = momentum * momentumMatrix[row][col] + (1.0 - momentum) * gradient;
        momentumMatrix[row][col] = update;

        double weight = weights[row][col];
        weight += learningRate * update;

        if (applyRegularization) {
//            weight += learningRate * (- (weight * l2Regularization));
            weight -= weight * lrTimesL2;
        }
        weights[row][col] = weight;
    }

    public final void incrementIteration() {
    }

    public double getLrTimesL2() {
        return lrTimesL2;
    }

    public void setLearningRate(double lr) {
        double l2 = this.lrTimesL2 / this.learningRate;
        this.learningRate = lr;
        this.lrTimesL2 = lr * l2;
    }
}
