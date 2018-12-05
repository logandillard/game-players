package games;

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

    public LayeredNNTD(NNLayerFullyConnectedTD[] layers) {
        this.layers = layers;
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
}
