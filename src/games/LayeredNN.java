package games;

import java.io.Serializable;

public class LayeredNN implements TDLearningNN, Serializable {
    private static final long serialVersionUID = 1L;
    private NNLayerFullyConnected[] layers;

    public LayeredNN(int[] unitsByLayer, ActivationFunction activationFunction,
            WeightInitializer initializer,
            double learningRate, double eligDecay,
            double l2Regularization, double l1Regularization) {
        if (unitsByLayer.length < 2) {
            throw new RuntimeException("Need at least 2 layers");
        }

        layers = new NNLayerFullyConnected[unitsByLayer.length - 1];
        for (int layer = 0; layer < unitsByLayer.length - 1; layer++) {
            layers[layer] = new NNLayerFullyConnected(unitsByLayer[layer], unitsByLayer[layer + 1],
                    activationFunction, initializer,
                    learningRate, eligDecay, l2Regularization, l1Regularization);
        }
    }

    public void reset() {
        for (NNLayerFullyConnected layer : layers) {
            layer.initEligTraces();
        }
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
        for (NNLayerFullyConnected layer : layers) {
            sb.append(layer.toString() + "\n");
        }
        return sb.toString();
    }

    public void setLearningRate(double learningRate) {
        for (NNLayerFullyConnected layer : layers) {
            layer.setLearningRate(learningRate);
        }
    }

    public void setEligDecay(double eligDecay) {
        for (NNLayerFullyConnected layer : layers) {
            layer.setEligDecay(eligDecay);
        }
    }

    public void setL2Regularization(double l2Regularization) {
        for (NNLayerFullyConnected layer : layers) {
            layer.setL2Regularization(l2Regularization);
        }
    }

    public void setL1Regularization(double l1Regularization) {
        for (NNLayerFullyConnected layer : layers) {
            layer.setL1Regularization(l1Regularization);
        }
    }
}
