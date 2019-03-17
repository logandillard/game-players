package com.dillard.nn;

import java.io.Serializable;

public class NNLayerResidual implements Serializable, NNLayer {
    private static final long serialVersionUID = 1L;
    private NNLayer[] layers;
    private double[] inputValues;

    public NNLayerResidual(NNLayer[] layers) {
        this.layers = layers;
    }

    @Override
    public NNLayerResidual clone() {
        NNLayer[] l = new NNLayer[this.layers.length];
        for (int i=0; i<l.length; i++) {
            l[i] = layers[i].clone();
        }
        return new NNLayerResidual(l);
    }

    public NNLayerResidual cloneWeights() {
        NNLayer[] l = new NNLayer[this.layers.length];
        for (int i=0; i<l.length; i++) {
            l[i] = layers[i].cloneWeights();
        }
        return new NNLayerResidual(l);
    }

    public double[] activate(double[] inputValues) {
        this.inputValues = inputValues;
        for (NNLayer layer : layers) {
            inputValues = layer.activate(inputValues);
        }
        // TODO We adopt the second nonlinearity after the addition
        for (int i=0; i<inputValues.length; i++) {
            inputValues[i] += this.inputValues[i];
        }
        return inputValues;
    }

    public double[] backprop(double[] errorGradient) {
        double[] nextLayerErrorGradient = errorGradient;
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            nextLayerErrorGradient = layers[layer].backprop(nextLayerErrorGradient);
        }

        // pass the output error gradients back directly
        for (int i=0; i<nextLayerErrorGradient.length; i++) {
            nextLayerErrorGradient[i] += errorGradient[i];
        }

        return nextLayerErrorGradient;
    }

    public double[] accumulateGradients(double[] errorGradient) {
        double[] nextLayerErrorGradient = errorGradient;
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            nextLayerErrorGradient = layers[layer].accumulateGradients(nextLayerErrorGradient);
        }

        // pass the output error gradients back directly
        for (int i=0; i<nextLayerErrorGradient.length; i++) {
            nextLayerErrorGradient[i] += errorGradient[i];
        }

        return nextLayerErrorGradient;
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

    public NNLayer[] getLayers() {
        return layers;
    }

}
