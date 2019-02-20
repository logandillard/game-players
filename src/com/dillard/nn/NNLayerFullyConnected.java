package com.dillard.nn;

import java.io.Serializable;
import java.util.Arrays;

public class NNLayerFullyConnected implements NNLayer, Serializable {
    private static final long serialVersionUID = 1L;
    private final int numInputs;
    private final int numOutputs;
    private final double[][] weights;
    private double[] inputValues;
    private double[] outputValues;
    private final ActivationFunction activationFunction;
    private final ADAMOptimizerMatrix optimizer;

    public NNLayerFullyConnected(int numInputs, int numOutputs,
            ActivationFunction activationFunction,
            WeightInitializer initializer,
            double learningRate, double l2Regularization) {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.activationFunction = activationFunction;
        weights = new double[numInputs + 1][numOutputs]; // + 1 for biases
        optimizer = new ADAMOptimizerMatrix(numInputs + 1, numOutputs, // + 1 for biases
                learningRate, l2Regularization);
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

    public NNLayerFullyConnected(int numInputs, int numOutputs, double[][] weights, double[] inputValues,
            double[] outputValues, ActivationFunction activationFunction, ADAMOptimizerMatrix optimizer) {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.weights = weights;
        this.inputValues = inputValues;
        this.outputValues = outputValues;
        this.activationFunction = activationFunction;
        this.optimizer = optimizer;
    }

    @Override
    public NNLayerFullyConnected clone() {
        return new NNLayerFullyConnected(numInputs, numOutputs,
                Utils.copyArray2D(weights),
                inputValues == null ? null : inputValues.clone(),
                outputValues == null ? null : outputValues.clone(),
                activationFunction,
                optimizer.copyNew());
    }

    @Override
    public double[] activate(double[] inputValues) {
        this.inputValues = inputValues;
        Arrays.fill(outputValues, 0.0);
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
        return outputValues;
    }

    @Override
    public double[] backprop(double[] errorGradient) {
        double[] inputNodeGradient = new double[numInputs];

        double[] outputDerivative = new double[numOutputs];
        for (int output=0; output<numOutputs; output++) {
            outputDerivative[output] = errorGradient[output] * activationFunction.derivative(outputValues[output]);
        }

        for (int input=0; input<numInputs; input++) {
            for (int output=0; output<numOutputs; output++) {
                double gradient = inputValues[input] * outputDerivative[output];

                // ADAM update
                optimizer.update(weights, input, output, gradient, true);

                double currentWeight = weights[input][output]; // weights[input][output];
                // sum the derivative of the output with respect to the input for a previous layer to use
                inputNodeGradient[input] += outputDerivative[output] * currentWeight; // TODO update after regularization?
            }
        }

        // biases
        for (int output=0; output<numOutputs; output++) {
            // ADAM update
            optimizer.update(weights, numInputs, output, outputDerivative[output], true); // no regularization on biases
        }

        optimizer.incrementIteration();

        return inputNodeGradient;
    }

    @Override
    public String toString() {
        return NNDiagnostics.weightsToString(weights);
    }
}
