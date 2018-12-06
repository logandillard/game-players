package com.dillard.nn;

public class NNLayerMaxPooling implements NNLayer {
    private final int numInputs;
    private final int layerSize;
    private final int numOutputs;
    private double[] inputValues;
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


    @Override
    public double[] activate(double[] inputValues) {
        this.inputValues = inputValues;
        this.outputValues = new double[numInputs/layerSize];
        this.selectedOutputValues = new int[numOutputs];

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

}
