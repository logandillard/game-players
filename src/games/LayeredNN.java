package games;

import java.io.Serializable;

public class LayeredNN implements Serializable {
    private static final long serialVersionUID = 1L;
    private NNLayerFullyConnected[] layers;

    public static LayeredNN buildFullyConnected(int[] unitsByLayer, ActivationFunction activationFunction,
            WeightInitializer initializer,
            double learningRate, double l2Regularization, double l1Regularization) {
        if (unitsByLayer.length < 2) {
            throw new RuntimeException("Need at least 2 layers");
        }

        NNLayerFullyConnected[] layers = new NNLayerFullyConnected[unitsByLayer.length - 1];
        for (int layer = 0; layer < unitsByLayer.length - 1; layer++) {
            layers[layer] = new NNLayerFullyConnected(unitsByLayer[layer], unitsByLayer[layer + 1],
                    activationFunction, initializer,
                    learningRate, l2Regularization, l1Regularization);
        }

        return new LayeredNN(layers);
    }

    public LayeredNN(NNLayerFullyConnected[] layers) {
        this.layers = layers;
    }

    public double[] activate(double[] inputValues) {
        for (NNLayerFullyConnected layer : layers) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (NNLayerFullyConnected layer : layers) {
            sb.append(layer.toString() + "\n");
        }
        return sb.toString();
    }
}
