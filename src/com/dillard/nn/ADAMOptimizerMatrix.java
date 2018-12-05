package com.dillard.nn;

/**
 * ADAM optimizer for a neural network weight matrix. Does ADAM SGD updates. Not aware of biases.
 */
public final class ADAMOptimizerMatrix {
    private double[][] adamM;
    private double[][] adamV;
    private final double beta1 = 0.9;
    private final double beta2 = 0.999;
    private double beta1t = beta1;
    private double beta2t = beta2;
    private double learningRate;
    private double l1Regularization;
    private double l2Regularization;

    public ADAMOptimizerMatrix(int weightRows, int weightCols,
            double learningRate, double l2Regularization, double l1Regularization) {
        this.learningRate = learningRate;
        this.l2Regularization = l2Regularization;
        this.l1Regularization = l1Regularization;
        adamM = new double[weightRows][weightCols]; // + 1 for biases
        adamV = new double[weightRows][weightCols]; // + 1 for biases
    }

    public final void update(double[][] weights, int row, int col, double gradient, boolean applyRegularization) {
        // ADAM update
        adamM[row][col] = beta1 * adamM[row][col] +
                (1.0 - beta1) * gradient;
        adamV[row][col] = beta2 * adamV[row][col] +
                (1.0 - beta2) * gradient * gradient;

        double adjustedM = adamM[row][col] / (1.0 - beta1t);
        double adjustedV = adamV[row][col] / (1.0 - beta2t);

        weights[row][col] += learningRate * (adjustedM / (Math.sqrt(adjustedV) + 0.00000001));

        if (applyRegularization) {
            double currentWeight = weights[row][col];
            weights[row][col] += learningRate * (
                    - (currentWeight * l2Regularization)
                    - (l1Regularization * Math.signum(currentWeight)));
        }
    }

    public final void incrementIteration() {
        beta1t *= beta1;
        beta2t *= beta2;
    }
}
