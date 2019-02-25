package com.dillard.nn;

import java.io.Serializable;

/**
 * ADAM optimizer for a neural network weight matrix. Does ADAM SGD updates. Not aware of biases.
 */
public final class ADAMOptimizerMatrix implements Serializable {
    private static final long serialVersionUID = 1L;
    private final double[][] adamM;
    private final double[][] adamV;
    private final double beta1 = 0.9;
    private final double beta2 = 0.999;
    private double beta1t = beta1;
    private double beta2t = beta2;
    private double beta1tMult = 1.0 / (1.0 - beta1t); // precomputed for speed
    private double beta2tMult = 1.0 / (1.0 - beta2t); // precomputed for speed
    private final double learningRate;
    private final double lrTimesL2; // precomputed for speed

    public ADAMOptimizerMatrix(int weightRows, int weightCols,
            double learningRate, double l2Regularization) {
        this.learningRate = learningRate;
        this.lrTimesL2 = learningRate * l2Regularization;
        adamM = new double[weightRows][weightCols];
        adamV = new double[weightRows][weightCols];
    }

    /** Not a clone, just a new one with the same parameters. Does not copy state. */
    public ADAMOptimizerMatrix copyNew() {
        return new ADAMOptimizerMatrix(adamM.length, adamM[0].length, learningRate, lrTimesL2 / learningRate);
    }

    public final void update(final double[][] weights, int row, int col, double gradient, boolean applyRegularization) {
        // ADAM update
        adamM[row][col] = beta1 * adamM[row][col] + (1.0 - beta1) * gradient;
        adamV[row][col] = beta2 * adamV[row][col] + (1.0 - beta2) * gradient * gradient;

        double adjustedM = adamM[row][col] * beta1tMult;
        double adjustedV = adamV[row][col] * beta2tMult;

        double weight = weights[row][col];
        weight += learningRate * (adjustedM / (Math.sqrt(adjustedV) + 0.00000001));

        if (applyRegularization) {
//            weight += learningRate * (- (weight * l2Regularization));
            weight -= weight * lrTimesL2;
        }
        weights[row][col] = weight;
    }

    public final double getUpdate(int row, int col, double gradient) {
        // ADAM update
        adamM[row][col] = beta1 * adamM[row][col] + (1.0 - beta1) * gradient;
        adamV[row][col] = beta2 * adamV[row][col] + (1.0 - beta2) * gradient * gradient;

        double adjustedM = adamM[row][col] * beta1tMult;
        double adjustedV = adamV[row][col] * beta2tMult;

        double update = learningRate * (adjustedM / (Math.sqrt(adjustedV) + 0.00000001));
        return update;
    }

    public final void incrementIteration() {
        beta1t *= beta1;
        beta2t *= beta2;

        // precompute these inverses to speed up using them later
        beta1tMult = 1.0 / (1.0 - beta1t);
        beta2tMult = 1.0 / (1.0 - beta2t);
    }

    public double getLrTimesL2() {
        return lrTimesL2;
    }
}
