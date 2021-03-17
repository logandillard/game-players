package com.dillard.nn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NNLayerFullyConnected implements NNLayer, Serializable {
    private static final long serialVersionUID = 1L;
    private static final double VARIANCE_EPSILON = 0.0001;
    private final int numInputs;
    private final int numOutputs;
    private final double[][] weights;
    private double rollingMomentum = 0.95;
    private double[] inputValues;
    private double[] outputValues;
    private double[][] batchInputValues;
    private double[][] batchOutputValues;
    private ActivationFunction activationFunction;
    private final OptimizerMatrix optimizer;

    public NNLayerFullyConnected(int numInputs, int numOutputs,
            ActivationFunction activationFunction,
            WeightInitializer initializer,
            double learningRate, double l2Regularization) {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.activationFunction = activationFunction;
        weights = new double[numInputs + 2][numOutputs]; // + 1 for biases
        optimizer = new MomentumOptimizerMatrix(numInputs + 1, numOutputs, // + 1 for biases
                learningRate, l2Regularization, 0.9 /* momentum */);
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

    public NNLayerFullyConnected(int numInputs, int numOutputs, double[][] weights,
            double[] rollingVariances, double[] rollingMeans, double rollingMomentum,
            ActivationFunction activationFunction, OptimizerMatrix optimizer) {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.weights = weights;
        this.rollingMomentum = rollingMomentum;
        this.activationFunction = activationFunction;
        this.optimizer = optimizer;
    }

    @Override
    public NNLayerFullyConnected clone() {
        return new NNLayerFullyConnected(numInputs, numOutputs,
                Utils.copyArray2D(weights),
                inputValues == null ? null : inputValues.clone(),
                outputValues == null ? null : outputValues.clone(),
                rollingMomentum,
                activationFunction,
                optimizer.copyNew());
    }

    @Override
    public NNLayerFullyConnected cloneWeights() {
        return new NNLayerFullyConnected(numInputs, numOutputs,
                Utils.copyArray2D(weights),
                null, // inputValues
                new double[numOutputs], // outputValues
                rollingMomentum,
                activationFunction,
                null); // optimizer
    }

    @Override
    public double[] activate(double[] inputValues) {
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

        // activation function
        for (int output=0; output<numOutputs; output++) {
            outputValues[output] = activationFunction.activate(outputValues[output]);
        }
        this.inputValues = inputValues;
        this.outputValues = outputValues;
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

                optimizer.update(weights, input, output, gradient, true);

                // sum the derivative of the output with respect to the input for a previous layer to use
                inputNodeGradient[input] += outputDerivative[output] * weights[input][output];
            }
        }

        // biases
        for (int output=0; output<numOutputs; output++) {
            optimizer.update(weights, numInputs, output, outputDerivative[output], false); // no regularization on biases
        }

        optimizer.incrementIteration();

        return inputNodeGradient;
    }

    @Override
    public List<double[]> batchActivate(List<double[]> batchInputs) {
        final int nBatch = batchInputs.size();
        this.batchInputValues = new double[nBatch][numInputs];
        this.batchOutputValues = new double[nBatch][numOutputs];
        List<double[]> outputList = new ArrayList<>(nBatch);
        for (int i=0; i<nBatch; i++) {
            double[] inputs = batchInputs.get(i);
            double[] outputs = this.activate(inputs);
            batchInputValues[i] = inputs;
            batchOutputValues[i] = outputs;
            outputList.add(outputs);
        }

        return outputList;
    }

    @Override
    public List<double[]> batchBackprop(List<double[]> batchErrorGradients) {
        // TODO
        final int batchN = batchErrorGradients.size();
        List<double[]> batchInputNodeGradients = new ArrayList<>(batchN);

        List<double[]> batchOutputDerivatives = new ArrayList<>(batchN);
        double[] outputDerivativesSum = new double[numOutputs];
        for (double[] errorGradients : batchErrorGradients) {
            double[] outputDerivative = new double[numOutputs];
            for (int output=0; output<numOutputs; output++) {
                double val = errorGradients[output] * activationFunction.derivative(outputValues[output]);
                outputDerivative[output] = val;
                outputDerivativesSum[output] += val;
            }
            batchOutputDerivatives.add(outputDerivative);
        }

        // Compute bias gradients
        // dbeta = np.sum(dy, axis=0)
        double[] biasGradients = new double[numOutputs];
        for (double[] errorGradients : batchOutputDerivatives) {
            for (int i=0; i<numOutputs; i++) {
                biasGradients[i] += errorGradients[i];
            }
        }

        // Compute BN scaler gradients
        // dgamma = np.sum((h - mu) * (var + eps)**(-1. / 2.) * dy, axis=0)
        double[] scalerGradients = new double[numOutputs];
        for (int instanceIndex=0; instanceIndex<batchN; instanceIndex++) {
            double[] errorGradients = batchOutputDerivatives.get(instanceIndex);
            for (int i=0; i<numOutputs; i++) {
                scalerGradients[i] += batchActivation.batchNormalizedH.get(instanceIndex)[i] * errorGradients[i];
            }
        }


        // Compute batch normalization gradients for weights
        // dh = (1. / N) * gamma * (var + eps)**(-1. / 2.) *
        //      (N * dy - np.sum(dy, axis=0) - (h - mu) * (var + eps)**(-1.0) * np.sum(dy * (h - mu), axis=0))
        double[] sumDyxHmMu = new double[numOutputs];
        for (int instanceIndex=0; instanceIndex<batchN; instanceIndex++) {
            for (int i=0; i<numOutputs; i++) {
                sumDyxHmMu[i] += batchOutputDerivatives.get(instanceIndex)[i] * (batchActivation.batchNormalizedH.get(instanceIndex)[i] - batchActivation.means[i]);
            }
        }
        double[][] activationGradients = new double[batchN][numOutputs];
        for (int instanceIndex=0; instanceIndex<batchN; instanceIndex++) {
            for (int i=0; i<numOutputs; i++) {
                activationGradients[instanceIndex][i] +=
                        (1.0 / batchN) * weights[numInputs][i] * batchActivation.inverseStdDev[i] *
                        batchN * batchOutputDerivatives.get(instanceIndex)[i] - outputDerivativesSum[i] - (
                                (batchActivation.batchNormalizedH.get(instanceIndex)[i] - batchActivation.means[i]) *
                                 batchActivation.inverseStdDev[i] * batchActivation.inverseStdDev[i] *
                                 sumDyxHmMu[i]
                                );
            }
        }

        // Update BN biases
        for (int output=0; output<numOutputs; output++) {
            optimizer.update(weights, numInputs + 1, output, biasGradients[output], false); // no regularization on biases
        }

        // Update BN scalers
        for (int output=0; output<numOutputs; output++) {
            optimizer.update(weights, numInputs, output, scalerGradients[output], true);
        }

        // Update weights
        double[][] weightGradients = new double[numInputs][numOutputs];
        for (int instanceIndex=0; instanceIndex<batchN; instanceIndex++) {
            double[] inputNodeGradient = new double[numInputs];
            for (int input=0; input<numInputs; input++) {
                for (int output=0; output<numOutputs; output++) {
                    double gradient = batchActivation.inputs.get(instanceIndex)[input] * activationGradients[instanceIndex][output];

                    // Sum weight gradients across instances
                    weightGradients[input][output] += gradient;

                    // sum the derivative of the output with respect to the input for a previous layer to use
                    inputNodeGradient[input] += activationGradients[instanceIndex][output] * weights[input][output];
                }
            }
            batchInputNodeGradients.add(inputNodeGradient);
        }
        for (int input=0; input<numInputs; input++) {
            for (int output=0; output<numOutputs; output++) {
                optimizer.update(weights, input, output, weightGradients[input][output], true);
            }
        }

        optimizer.incrementIteration();

        return batchInputNodeGradients;
    }

    public double[] accumulateGradients(double[] errorGradient) {
        // This is copied from backprop() above, but we don't apply the update from the optimizer, we just store the update
        double[] inputNodeGradient = new double[numInputs];

        double[] outputDerivative = new double[numOutputs];
        for (int output=0; output<numOutputs; output++) {
            outputDerivative[output] = errorGradient[output] * activationFunction.derivative(outputValues[output]);
        }

        for (int input=0; input<numInputs; input++) {
            for (int output=0; output<numOutputs; output++) {
                double gradient = inputValues[input] * outputDerivative[output];

                accumulatedGradients[input][output] += gradient;

                // sum the derivative of the output with respect to the input for a previous layer to use
                inputNodeGradient[input] += outputDerivative[output] * weights[input][output];
            }
        }

        // biases
        for (int output=0; output<numOutputs; output++) {
            accumulatedGradients[numInputs][output] += outputDerivative[output];
        }

        accumulatedGradientCount++;

        return inputNodeGradient;
    }

    public void applyAccumulatedGradients() {
        for (int input=0; input<numInputs; input++) {
            for (int output=0; output<numOutputs; output++) {
//                optimizer.update(weights, input, output, accumulatedGradients[input][output] / accumulatedGradientCount, true);
                optimizer.update(weights, input, output, accumulatedGradients[input][output], true);
            }
        }
        // biases
        for (int output=0; output<numOutputs; output++) {
//            optimizer.update(weights, numInputs, output, accumulatedGradients[numInputs][output] / accumulatedGradientCount, false);
            optimizer.update(weights, numInputs, output, accumulatedGradients[numInputs][output], false);
        }

        optimizer.incrementIteration();

        // Clear accumulated gradients
        for (int i=0; i<accumulatedGradients.length; i++) {
            Arrays.fill(accumulatedGradients[i], 0);
        }
        accumulatedGradientCount = 0;
    }

    @Override
    public String toString() {
        return NNDiagnostics.weightsToString(weights);
    }

    @Override
    public void setLearningRate(double lr) {
        optimizer.setLearningRate(lr);
    }

    @Override
    public ActivationFunction getActivationFunction() {
        return activationFunction;
    }

    @Override
    public void setActivationFunction(ActivationFunction activationFunction) {
        this.activationFunction = activationFunction;
    }
}
