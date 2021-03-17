package com.dillard.nn;

import java.io.Serializable;

public class LayeredNNTD implements TDLearningNN, Serializable {
    private static final long serialVersionUID = 1L;
    private NNLayerFullyConnectedTD[] layers;

    public static LayeredNNTD buildFullyConnected(int[] unitsByLayer, ActivationFunction activationFunction,
            WeightInitializer initializer,
            double learningRate, double eligDecay,
            double l2Regularization, double l1Regularization) {
        if (unitsByLayer.length < 2) {
            throw new RuntimeException("Need at least 2 layers");
        }

        NNLayerFullyConnectedTD[] layers = new NNLayerFullyConnectedTD[unitsByLayer.length - 1];
        for (int layer = 0; layer < unitsByLayer.length - 1; layer++) {
            layers[layer] = new NNLayerFullyConnectedTD(unitsByLayer[layer], unitsByLayer[layer + 1],
                    activationFunction, initializer,
                    learningRate, eligDecay, l2Regularization, l1Regularization);
        }

        return new LayeredNNTD(layers);
    }

    public static LayeredNNTD buildFullyConnectedSELU(int[] unitsByLayer,
            ActivationFunction finalLayerActivationFunction,
            double learningRate, double eligDecay,
            double l2Regularization, double l1Regularization) {
        if (unitsByLayer.length < 2) {
            throw new RuntimeException("Need at least 2 layers");
        }

        NNLayerFullyConnectedTD[] layers = new NNLayerFullyConnectedTD[unitsByLayer.length - 1];
        for (int layer = 0; layer < unitsByLayer.length - 1; layer++) {
            ActivationFunction activationFunction = layer < unitsByLayer.length - 1
                    ? new ActivationFunctionSeLU()
                    : finalLayerActivationFunction;

            // 1/n is the recommended variance for SELU
            // I think this is called glorot? For ReLU it should be 2 / N, but SELU is different
            double variance = Math.sqrt(1.0/(unitsByLayer[layer]));
            WeightInitializer initializer = new WeightInitializerGaussianFixedVariance(variance);

            layers[layer] = new NNLayerFullyConnectedTD(unitsByLayer[layer], unitsByLayer[layer + 1],
                    activationFunction, initializer,
                    learningRate, eligDecay, l2Regularization, l1Regularization);
        }

        return new LayeredNNTD(layers);
    }

    public LayeredNNTD(NNLayerFullyConnectedTD[] layers) {
        this.layers = layers;
    }

    public LayeredNNTD(LayeredNNTD o) {
        this.layers = new NNLayerFullyConnectedTD[o.layers.length];
        for (int i=0; i<layers.length; i++) {
            layers[i] = new NNLayerFullyConnectedTD(o.layers[i]);
        }
    }

    public void reset() {
        for (NNLayerFullyConnectedTD layer : layers) {
            layer.initEligTraces();
        }
    }

    public double[] activate(double[] inputValues) {
        for (NNLayerFullyConnectedTD layer : layers) {
            inputValues = layer.activate(inputValues);
        }
        return inputValues;
    }

    public void updateElig() {
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            layers[layer].updateElig();
        }
    }

    public void tdLearn(double[] errors) {
        double[] nextLayerErrors = errors;
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            nextLayerErrors = layers[layer].tdLearn(nextLayerErrors);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (NNLayerFullyConnectedTD layer : layers) {
            sb.append(layer.toString() + "\n");
        }
        return sb.toString();
    }

    public void setLearningRate(double lr) {
        for (NNLayerFullyConnectedTD layer : layers) {
            layer.setLearningRate(lr);
        }
    }

    public void setEligDecayRate(double decay) {
        for (NNLayerFullyConnectedTD layer : layers) {
            layer.setEligDecayRate(decay);
        }
    }

    public void setL2Regularization(double l2) {
        for (NNLayerFullyConnectedTD layer : layers) {
            layer.setL2Regularization(l2);
        }
    }

    public void setL1Regularization(double l1) {
        for (NNLayerFullyConnectedTD layer : layers) {
            layer.setL1Regularization(l1);
        }
    }

    public NNLayerFullyConnectedTD[] getLayers() {
        return layers;
    }
}
