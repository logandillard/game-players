package com.dillard.nn;

import java.io.Serializable;

public class LayeredNN implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private NNLayer[] layers;

    public static LayeredNN buildFullyConnected(int[] unitsByLayer, ActivationFunction activationFunction,
            WeightInitializer initializer,
            double learningRate, double l2Regularization) {
        if (unitsByLayer.length < 2) {
            throw new RuntimeException("Need at least 2 layers");
        }

        NNLayer[] layers = new NNLayer[unitsByLayer.length - 1];
        for (int layer = 0; layer < unitsByLayer.length - 1; layer++) {
            layers[layer] = new NNLayerFullyConnected(unitsByLayer[layer], unitsByLayer[layer + 1],
                    activationFunction, initializer,
                    learningRate, l2Regularization);
        }

        return new LayeredNN(layers);
    }

    public LayeredNN(NNLayer[] layers) {
        this.layers = layers;
    }

    @Override
    public LayeredNN clone() {
        NNLayer[] l = new NNLayer[this.layers.length];
        for (int i=0; i<l.length; i++) {
            l[i] = layers[i].clone();
        }
        return new LayeredNN(l);
    }

    public LayeredNN cloneWeights() {
        NNLayer[] l = new NNLayer[this.layers.length];
        for (int i=0; i<l.length; i++) {
            l[i] = layers[i].cloneWeights();
        }
        return new LayeredNN(l);
    }

    public double[] activate(double[] inputValues) {
        for (NNLayer layer : layers) {
            inputValues = layer.activate(inputValues);
        }
        return inputValues;
    }

    public void backprop(double[] errorGradient) {
        double[] nextLayerErrorGradient = errorGradient;
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            nextLayerErrorGradient = layers[layer].backprop(nextLayerErrorGradient);
        }
    }

    public void accumulateGradients(double[] errorGradient) {
        double[] nextLayerErrorGradient = errorGradient;
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            nextLayerErrorGradient = layers[layer].accumulateGradients(nextLayerErrorGradient);
        }
    }

    public void applyAccumulatedGradients() {
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            layers[layer].applyAccumulatedGradients();
        }
    }

    public void setLearningRate(double lr) {
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            layers[layer].setLearningRate(lr);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (NNLayer layer : layers) {
            sb.append(layer.toString() + "\n");
        }
        return sb.toString();
    }
}
