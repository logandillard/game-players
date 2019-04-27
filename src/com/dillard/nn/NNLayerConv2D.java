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
    private double[][] accumulatedGradients;
    private int accumulatedGradientCount = 0;
    private double[] inputValues;
    private double[] outputValues;
    private final ActivationFunction activationFunction;
    private final ADAMOptimizerMatrix optimizer;

    public NNLayerConv2D(int inputNumRows, int inputNumCols, int inputNumLayers,
            ActivationFunction activationFunction, WeightInitializer initializer,
            int numFilters, int width, int height, int depth, int stride, int padding, double paddingValue,
            double learningRate, double l2Regularization) {
        this.inputNumRows = inputNumRows;
        this.inputNumCols = inputNumCols;
//        this.inputNumLayers = inputNumLayers;
        this.numInputs = inputNumRows * inputNumCols * inputNumLayers;

        this.numFilters = numFilters;
        this.activationFunction = activationFunction;

        //  (W − F + 2P)/ S + 1
        numOutputRowsPerFilter = ((inputNumRows - height + 2*padding) / stride) + 1;
        numOutputColsPerFilter = ((inputNumCols - width  + 2*padding) / stride) + 1;
        // TODO throw an exception if these don't fit right
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
                learningRate, l2Regularization);
        weights = new double[numFilters][width * height * depth + 1]; // + 1 for biases
        accumulatedGradients = new double[numFilters][width * height * depth + 1]; // + 1 for biases
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

    NNLayerConv2D(int numInputs, int inputNumRows, int inputNumCols, int numOutputs, int numFilters,
            int numOutputsPerFilter, int numOutputRowsPerFilter, int numOutputColsPerFilter, int width, int height,
            int depth, int stride, int padding, double paddingValue, double[][] weights, double[] inputValues,
            double[] outputValues, ActivationFunction activationFunction, ADAMOptimizerMatrix optimizer) {
        this.numInputs = numInputs;
        this.inputNumRows = inputNumRows;
        this.inputNumCols = inputNumCols;
        this.numOutputs = numOutputs;
        this.numFilters = numFilters;
        this.numOutputsPerFilter = numOutputsPerFilter;
        this.numOutputRowsPerFilter = numOutputRowsPerFilter;
        this.numOutputColsPerFilter = numOutputColsPerFilter;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.stride = stride;
        this.padding = padding;
        this.paddingValue = paddingValue;
        this.weights = weights;
        this.inputValues = inputValues;
        this.outputValues = outputValues;
        this.activationFunction = activationFunction;
        this.optimizer = optimizer;
        accumulatedGradients = new double[numFilters][width * height * depth + 1]; // + 1 for biases
    }

    @Override
    public NNLayerConv2D clone() {
        return new NNLayerConv2D(
                numInputs, inputNumRows, inputNumCols, numOutputs, numFilters,
                numOutputsPerFilter, numOutputRowsPerFilter, numOutputColsPerFilter, width, height,
                depth, stride, padding, paddingValue,
                Utils.copyArray2D(weights),
                inputValues.clone(),
                outputValues.clone(),
                activationFunction,
                optimizer.copyNew()
                );
    }

    @Override
    public NNLayerConv2D cloneWeights() {
        return new NNLayerConv2D(
                numInputs, inputNumRows, inputNumCols, numOutputs, numFilters,
                numOutputsPerFilter, numOutputRowsPerFilter, numOutputColsPerFilter, width, height,
                depth, stride, padding, paddingValue,
                Utils.copyArray2D(weights),
                null, // inputValues.clone(),
                new double[numOutputs], // outputValues.clone(),
                activationFunction,
                null // optimizer.copyNew()
                );
    }

    public double[] activate(final double[] inputValues) {
        this.inputValues = inputValues;
        double[] outputValues = new double[numOutputs];
        double[][] filterOutputs = new double[numFilters][];

        for (int filter=0; filter<weights.length; filter++) {

            double[] filterWeights = weights[filter];
            double[] filterOutput = new double[numOutputsPerFilter];
            int filterOutputIdx = 0;
            for (int startRow=0 - padding; startRow<inputNumRows + padding - height + 1; startRow += stride) {
                for (int startCol=0 - padding; startCol<inputNumCols + padding - width + 1; startCol += stride) {

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

                    // bias
                    filterValue += filterWeights[depth*width*height];
                    filterOutput[filterOutputIdx] = activationFunction.activate(filterValue);
                    filterOutputIdx++;
                }
            }
            filterOutputs[filter] = filterOutput;
            // Copy filter outputs into the single output array
            System.arraycopy(filterOutput, 0,
                    outputValues, numOutputsPerFilter * filter,
                    filterOutput.length);
        }
        this.outputValues = outputValues;
        return outputValues;
    }

    public double[] backprop(double[] errorGradient) {
        double[] inputNodeGradient = new double[numInputs];

        double[] outputDerivatives = new double[errorGradient.length];
        for (int output=0; output<numOutputs; output++) {
            outputDerivatives[output] = errorGradient[output] * activationFunction.derivative(outputValues[output]);
        }

        for (int filter=0; filter<numFilters; filter++) {

            for (int startRow=0 - padding, outputRow=0; startRow<inputNumCols + padding - height + 1; startRow += stride, outputRow++) {
                for (int startCol=0 - padding, outputCol=0; startCol<inputNumCols + padding - width + 1; startCol += stride, outputCol++) {

                    double outputDerivative = outputDerivatives[filter*numOutputRowsPerFilter*numOutputColsPerFilter +
                                                                outputRow*numOutputColsPerFilter + outputCol];
                    if (outputDerivative != 0.0) {
                        for (int filterRow = 0; filterRow<height; filterRow++) {
                            int inputRow = filterRow + startRow;
                            for (int filterCol = 0; filterCol<width; filterCol++) {
                                int inputCol = filterCol + startCol;

                                for (int layer=0; layer<depth; layer++) {

                                    double inputCellValue = paddingValue;
                                    boolean isNotPadding = inputRow >= 0 && inputCol >= 0 && inputRow < inputNumRows && inputCol < inputNumCols;
                                    if (isNotPadding) {
                                        inputCellValue = inputValues[layer*inputNumRows*inputNumCols + inputRow*inputNumCols + inputCol];
                                    }
                                    double gradient = outputDerivative * inputCellValue;
                                    optimizer.update(weights, filter, layer*width*height + filterRow*width + filterCol, gradient, true);

                                    // pass on the gradient to the next layer only if this is not padding
                                    if (isNotPadding) {
                                        double currentWeight = weights[filter][layer*width*height + filterRow*width + filterCol];
                                        inputNodeGradient[layer*inputNumRows*inputNumCols + inputRow*inputNumCols + inputCol] +=
                                                outputDerivative * currentWeight;
                                    }
                                }
                            }
                        }

                        // biases
                        optimizer.update(weights, filter, depth*width*height, outputDerivative, false);
                    }
                }
            }

        }

        optimizer.incrementIteration();

        return inputNodeGradient;
    }

    @Override
    public double[] accumulateGradients(double[] errorGradient) {
        double[] inputNodeGradient = new double[numInputs];

        double[] outputDerivatives = new double[errorGradient.length];
        for (int output=0; output<numOutputs; output++) {
            outputDerivatives[output] = errorGradient[output] * activationFunction.derivative(outputValues[output]);
        }

        for (int filter=0; filter<numFilters; filter++) {

            for (int startRow=0 - padding, outputRow=0; startRow<inputNumCols + padding - height + 1; startRow += stride, outputRow++) {
                for (int startCol=0 - padding, outputCol=0; startCol<inputNumCols + padding - width + 1; startCol += stride, outputCol++) {

                    double outputDerivative = outputDerivatives[filter*numOutputRowsPerFilter*numOutputColsPerFilter +
                                                                outputRow*numOutputColsPerFilter + outputCol];
                    if (outputDerivative != 0.0) {
                        for (int filterRow = 0; filterRow<height; filterRow++) {
                            int inputRow = filterRow + startRow;
                            for (int filterCol = 0; filterCol<width; filterCol++) {
                                int inputCol = filterCol + startCol;

                                for (int layer=0; layer<depth; layer++) {

                                    double inputCellValue = paddingValue;
                                    boolean isNotPadding = inputRow >= 0 && inputCol >= 0 && inputRow < inputNumRows && inputCol < inputNumCols;
                                    if (isNotPadding) {
                                        inputCellValue = inputValues[layer*inputNumRows*inputNumCols + inputRow*inputNumCols + inputCol];
                                    }
                                    double gradient = outputDerivative * inputCellValue;
                                    int weightCol = layer*width*height + filterRow*width + filterCol;
                                    accumulatedGradients[filter][weightCol] += gradient;

                                    // pass on the gradient to the next layer only if this is not padding
                                    if (isNotPadding) {
                                        double currentWeight = weights[filter][layer*width*height + filterRow*width + filterCol];
                                        inputNodeGradient[layer*inputNumRows*inputNumCols + inputRow*inputNumCols + inputCol] +=
                                                outputDerivative * currentWeight;
                                    }
                                }
                            }
                        }

                        // biases
                        int col = depth*width*height;
                        accumulatedGradients[filter][col] += outputDerivative;
                    }
                }
            }

        }

        accumulatedGradientCount++;

        return inputNodeGradient;
    }

    @Override
    public void applyAccumulatedGradients() {
        for (int filter=0; filter<numFilters; filter++) {
            for (int filterRow = 0; filterRow<height; filterRow++) {
                for (int filterCol = 0; filterCol<width; filterCol++) {
                    for (int layer=0; layer<depth; layer++) {
                        int weightCol = layer*width*height + filterRow*width + filterCol;

                        optimizer.update(weights, filter, weightCol,
                                accumulatedGradients[filter][weightCol] / accumulatedGradientCount, true);
                    }
                }
            }

            // biases
            int col = depth*width*height;
            optimizer.update(weights, filter, col,
                    accumulatedGradients[filter][col] / accumulatedGradientCount, true);
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

    public int getNumOutputs() {
        return numOutputs;
    }

    public int getNumFilters() {
        return numFilters;
    }


    @Override
    public void setLearningRate(double lr) {
        optimizer.setLearningRate(lr);
    }
}
