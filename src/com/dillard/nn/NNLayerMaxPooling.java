package com.dillard.nn;

public class NNLayerMaxPooling implements NNLayer {
    private final int numInputs;
    private final int layerSize;
    private final int numOutputs;
    private double[] outputValues;
    private int[] selectedOutputValues;

    public NNLayerMaxPooling(int numInputs, int layerSize) {
        this.layerSize = layerSize;
        this.numInputs = numInputs;

        if (numInputs / (double) layerSize != numInputs / layerSize) {
            throw new RuntimeException();
        }
        numOutputs = numInputs / layerSize;
    }

    public NNLayerMaxPooling(int numInputs, int layerSize, int numOutputs, double[] outputValues,
            int[] selectedOutputValues) {
        this.numInputs = numInputs;
        this.layerSize = layerSize;
        this.numOutputs = numOutputs;
        this.outputValues = outputValues;
        this.selectedOutputValues = selectedOutputValues;
    }

    @Override
    public NNLayerMaxPooling clone() {
        return new NNLayerMaxPooling(numInputs, layerSize, numOutputs,
                outputValues.clone(),
                selectedOutputValues.clone());
    }

    @Override
    public NNLayerMaxPooling cloneWeights() {
        return new NNLayerMaxPooling(numInputs, layerSize, numOutputs,
                new double[numOutputs], // outputValues.clone(),
                null); // selectedOutputValues.clone());
    }

    @Override
    public double[] activate(double[] inputValues) {
        double[] outputValues = new double[numOutputs];
        int[] selectedOutputValues = new int[numOutputs];

        for (int filter = 0; filter<numOutputs; filter++) {
            double max = -Double.MAX_VALUE;
            int maxIdx = -1;
            for (int i=0; i<layerSize; i++) {
                double value = inputValues[filter * layerSize + i];
                if (value > max) {
                    max = value;
                    maxIdx = i;
                }
            }
            selectedOutputValues[filter] = filter * layerSize + maxIdx;
            outputValues[filter] = max;
        }
        this.selectedOutputValues = selectedOutputValues;
        this.outputValues = outputValues;
        return outputValues;
    }

    @Override
    public double[] backprop(double[] errorGradient) {
        double[] inputNodeGradient = new double[numInputs];
        for (int filter = 0; filter<numOutputs; filter++) {
            inputNodeGradient[selectedOutputValues[filter]] = errorGradient[filter];
        }
        return inputNodeGradient;
    }

    @Override
    public double[] accumulateGradients(double[] errorGradient) {
        return backprop(errorGradient);
    }

    @Override
    public void applyAccumulatedGradients() {
    }

}
