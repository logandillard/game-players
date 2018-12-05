package com.dillard.nn;

import java.io.Serializable;
import java.util.Arrays;

public class NNLayerConv2D implements NNLayer, Serializable {
    private static final long serialVersionUID = 1L;
    private final int numInputs;
    private final int inputNumRows;
    private final int inputNumCols;
//    private final int inputNumLayers;
    private final int numOutputs;
    private final int numFilters;
    private final int numOutputsPerFilter;
    private final int numOutputRowsPerFilter;
    private final int numOutputColsPerFilter;
    private final int width;
    private final int height;
    private final int depth;
    private final int stride;
    private final int padding;
    private final double paddingValue;
    private double[][] weights;
    private double[] inputValues;
    private double[] outputValues;
    private final ActivationFunction activationFunction;
    private final ADAMOptimizerMatrix optimizer;

    public NNLayerConv2D(int inputNumRows, int inputNumCols, int inputNumLayers,
            ActivationFunction activationFunction, WeightInitializer initializer,
            int numFilters, int width, int height, int depth, int stride, int padding, double paddingValue,
            double learningRate, double l2Regularization, double l1Regularization) {
        this.inputNumRows = inputNumRows;
        this.inputNumCols = inputNumCols;
//        this.inputNumLayers = inputNumLayers;
        this.numInputs = inputNumRows * inputNumCols * inputNumLayers;

        this.numFilters = numFilters;
        this.activationFunction = activationFunction;

        //  (W − F + 2P)/ S + 1
        numOutputRowsPerFilter = ((inputNumRows - height + 2*padding) / stride) + 1;
        numOutputColsPerFilter = ((inputNumCols - width + 2*padding) / stride) + 1;
        numOutputsPerFilter = numOutputColsPerFilter * numOutputRowsPerFilter;
        numOutputs = numOutputsPerFilter * numFilters;

        this.width = width;
        this.height = height;
        this.depth = depth;
        this.stride = stride;
        this.padding = padding;
        this.paddingValue = paddingValue;

        if (inputNumLayers != depth) {
            // this means that they shouldn't be different parameters. but how to name them clearly?
            throw new RuntimeException();
        }

        optimizer = new ADAMOptimizerMatrix(numFilters, width * height * depth + 1,
                learningRate, l2Regularization, l1Regularization);
        weights = new double[numFilters][width * height * depth + 1]; // + 1 for biases
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

    public double[] activate(final double[] inputValues) {
        this.inputValues = inputValues;
        Arrays.fill(outputValues, 0.0);
        double[][] filterOutputs = new double[numFilters][];

        for (int filter=0; filter<weights.length; filter++) {

            double[] filterWeights = weights[filter];
            double[] filterOutput = new double[numOutputsPerFilter];
            int filterOutputIdx = 0;
            for (int startRow=0 - padding; startRow<inputNumCols + padding - height; startRow += stride) {
                for (int startCol=0 - padding; startCol<inputNumCols + padding - width; startCol += stride) {

                    double filterValue = 0;
                    for (int filterRow = 0; filterRow<height; filterRow++) {
                        int inputRow = filterRow + startRow;
                        for (int filterCol = 0; filterCol<width; filterCol++) {
                            int inputCol = filterCol + startCol;
                            for (int layer=0; layer<depth; layer++) {
                                double inputCellValue = paddingValue;
                                if (inputRow >= 0 && inputCol >= 0 && inputRow < inputNumRows && inputCol < inputNumCols) {
                                    inputCellValue = inputValues[layer*inputNumRows*inputNumCols + inputRow*inputNumCols + inputCol];
                                }

                                filterValue += filterWeights[layer*width*height + filterRow*width + filterCol] * inputCellValue;
                            }
                        }
                    }

                    filterOutput[filterOutputIdx] = activationFunction.activate(filterValue + filterWeights[depth*width*height]); // include the bias
                    filterOutputIdx++;
                }
            }
            filterOutputs[filter] = filterOutput;
            // Copy filter outputs into the single output array
            System.arraycopy(filterOutput, 0,
                    outputValues, numOutputsPerFilter * filter,
                    filterOutput.length);
        }

        return outputValues;
    }

    public double[] backprop(double[] errorGradient) {
        double[] inputNodeGradient = new double[numInputs];

        double[] outputDerivatives = new double[errorGradient.length];
        for (int output=0; output<numOutputs; output++) {
            outputDerivatives[output] = errorGradient[output] * activationFunction.derivative(outputValues[output]);
        }

        for (int filter=0; filter<numFilters; filter++) {

            for (int startRow=0 - padding, outputRow=0; startRow<inputNumCols + padding - height; startRow += stride, outputRow++) {
                for (int startCol=0 - padding, outputCol=0; startCol<inputNumCols + padding - width; startCol += stride, outputCol++) {

                    double outputDerivative = outputDerivatives[filter*numOutputRowsPerFilter*numOutputColsPerFilter +
                                                                outputRow*numOutputColsPerFilter + outputCol];

                    for (int filterRow = 0; filterRow<height; filterRow++) {
                        int inputRow = filterRow + startRow;
                        for (int filterCol = 0; filterCol<width; filterCol++) {
                            int inputCol = filterCol + startCol;

                            for (int layer=0; layer<depth; layer++) {

                                double inputCellValue = paddingValue;
                                if (inputRow >= 0 && inputCol >= 0 && inputRow < inputNumRows && inputCol < inputNumCols) {
                                    inputCellValue = inputValues[layer*inputNumRows*inputNumCols + inputRow*inputNumCols + inputCol];
                                }
                                double gradient = outputDerivative * inputCellValue;
                                optimizer.update(weights, filter, layer*width*height + filterRow*width + filterCol, gradient, true);

                                double currentWeight = weights[filter][layer*width*height + filterRow*width + filterCol];
                                inputNodeGradient[layer*inputNumRows*inputNumCols + inputRow*inputNumCols + inputCol] +=
                                        outputDerivative * currentWeight;
                            }
                        }
                    }

                    // biases
                    optimizer.update(weights, filter, depth*width*height, outputDerivative, false);
                }
            }

        }

        optimizer.incrementIteration();

        return inputNodeGradient;
    }

    @Override
    public String toString() {
        return NNDiagnostics.weightsToString(weights);
    }

    public int getNumOutputs() {
        return numOutputs;
    }
}