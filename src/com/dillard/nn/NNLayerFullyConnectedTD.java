package com.dillard.nn;

import java.io.Serializable;

/**
 * For TD (Temporal Difference) learning.
 */
public class NNLayerFullyConnectedTD implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int numInputs;
    private final int numOutputs;
    private double[][] weights;
    private double[][] eligTraces;
    private double[] inputValues;
    private double[] outputValues;
    private double learningRate;
    private double eligDecay;
    private double l2Regularization;
    private double l1Regularization;
    private final double beta1 = 0.9;
    private final double beta2 = 0.999;
    private double beta1t = beta1;
    private double beta2t = beta2;
    private double[][] adamM;
    private double[][] adamV;
    private ActivationFunction activationFunction;

    public NNLayerFullyConnectedTD(NNLayerFullyConnectedTD o) {
        this.numInputs = o.numInputs;
        this.numOutputs = o.numOutputs;
        activationFunction = o.activationFunction;
        this.weights = deepCopyDoubleMatrix(o.weights);
        this.eligTraces = deepCopyDoubleMatrix(o.eligTraces);
        this.learningRate = o.learningRate;
        this.eligDecay = o.eligDecay;
        l2Regularization = o.l2Regularization;
        l1Regularization = o.l1Regularization;
        beta1t = o.beta1t;
        beta2t = o.beta2t;
        adamM = deepCopyDoubleMatrix(o.adamM);
        adamV = deepCopyDoubleMatrix(o.adamV);
        outputValues = new double[numOutputs];
    }

    public NNLayerFullyConnectedTD(int numInputs, int numOutputs,
            ActivationFunction activationFunction,
            WeightInitializer initializer,
            double learningRate, double eligDecay,
            double l2Regularization, double l1Regularization) {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.activationFunction = activationFunction;
        this.eligDecay = eligDecay;
        this.learningRate = learningRate;
        this.l2Regularization = l2Regularization;
        this.l1Regularization = l1Regularization;
        weights = new double[numInputs + 1][numOutputs]; // + 1 for biases
        eligTraces = new double[numInputs + 1][numOutputs]; // + 1 for biases
        adamM = new double[numInputs + 1][numOutputs]; // + 1 for biases
        adamV = new double[numInputs + 1][numOutputs]; // + 1 for biases
        outputValues = new double[numOutputs];

        // initialize weights randomly
        initializeWeights(initializer);
    }

    private void initializeWeights(WeightInitializer initializer) {
        for (int input=0; input<weights.length; input++) {
            for (int output=0; output<weights[input].length; output++) {
                weights[input][output] = initializer.nextInitialWeight();
            }
        }
    }

    public void initEligTraces() {
        eligTraces = new double[numInputs + 1][numOutputs]; // + 1 for bias
    }

    public double[] activate(double[] inputValues) {
        this.inputValues = inputValues;
        double[] outputValues = new double[numOutputs];
        for (int input=0; input<numInputs; input++) {
            if (inputValues[input] == 0.0) {
                continue;
            }
            for (int output=0; output<numOutputs; output++) {
                outputValues[output] += inputValues[input] * weights[input][output];
            }
        }
        // biases
        for (int output=0; output<numOutputs; output++) {
            outputValues[output] += weights[numInputs][output];
        }
        // link function
        for (int output=0; output<numOutputs; output++) {
            outputValues[output] = activationFunction.activate(outputValues[output]);
        }
        this.outputValues = outputValues;
        return outputValues;
    }

    public void updateElig() {
        double[] outputDerivative = new double[numOutputs];
        for (int output=0; output<numOutputs; output++) {
            outputDerivative[output] = activationFunction.derivative(outputValues[output]);
        }

        for (int input=0; input<numInputs; input++) {
            for (int output=0; output<numOutputs; output++) {
                double gradient = inputValues[input] * outputDerivative[output];

                // SGD update
//                eligTraces[input][output] = (eligDecay * eligTraces[input][output]) + gradient;

                // ADAM update
                adamUpdateEligTraces(input, output, gradient);
            }
        }

        // biases
        for (int output=0; output<numOutputs; output++) {
            double gradient = outputDerivative[output];

            // SGD update
//            eligTraces[numInputs][output] = (eligDecay * eligTraces[numInputs][output]) + gradient;

            // ADAM update
            adamUpdateEligTraces(numInputs, output, gradient);
        }

        beta1t *= beta1;
        beta2t *= beta2;
    }

    private void adamUpdateEligTraces(int input, int output, double gradient) {
        // ADAM update
        adamM[input][output] = beta1 * adamM[input][output] +
                (1.0 - beta1) * gradient;
        adamV[input][output] = beta2 * adamV[input][output] +
                (1.0 - beta2) * gradient * gradient;

        double adjustedM = adamM[input][output] / (1.0 - beta1t);
        double adjustedV = adamV[input][output] / (1.0 - beta2t);

        eligTraces[input][output] = (eligDecay * eligTraces[input][output]) +
                (adjustedM / (Math.sqrt(adjustedV) + 0.00000001) );
    }

    public double[] tdLearn(double[] errors) {
        double[] inputNodeErrors = new double[numInputs + 1];
        for (int input=0; input<weights.length; input++) {
            for (int output=0; output<weights[input].length; output++) {
                inputNodeErrors[input] += errors[output] * weights[input][output];
                double weight = weights[input][output];

                double update = learningRate * (
                        errors[output] * eligTraces[input][output]
                        - weight * l2Regularization);
//                        - l1Regularization * Math.signum(weight));

                weights[input][output] += update;
            }
        }
        return inputNodeErrors;
    }

    public void setLearningRate(double lr) {
        this.learningRate = lr;
    }

    public void setEligDecayRate(double decay) {
        this.eligDecay = decay;
    }

    public void setL2Regularization(double l2) {
        this.l2Regularization = l2;
    }

    public void setL1Regularization(double l1) {
        this.l1Regularization = l1;
    }

    public void setActivationFunction(ActivationFunction activationFunction) {
        this.activationFunction = activationFunction;
    }

    private static double[][] deepCopyDoubleMatrix(double[][] input) {
        if (input == null)
            return null;
        double[][] result = new double[input.length][];
        for (int r = 0; r < input.length; r++) {
            result[r] = input[r].clone();
        }
        return result;
    }

    @Override
    public String toString() {
        return NNDiagnostics.weightsToString(weights);
    }
}
