package com.dillard.mnist;

import java.util.Arrays;
import java.util.List;

import com.dillard.nn.ActivationFunction;
import com.dillard.nn.ActivationFunctionTanH;
import com.dillard.nn.LayeredNN;
import com.dillard.nn.WeightInitializer;
import com.dillard.nn.WeightInitializerGaussianFixedVariance;

public class MNISTNNPredictor implements MNISTPredictor {
    private static final int NUM_OUTPUTS = 10;
    private LayeredNN nn;

    public static MNISTNNPredictor build() {
        double learningRate = 0.001, l2 = 0.0001;
        ActivationFunction activation = new ActivationFunctionTanH();
        WeightInitializer initializer = new WeightInitializerGaussianFixedVariance(1.0/196.0);
//        WeightInitializer initializer = new WeightInitializerGaussianFixedVariance(1.0/49.0);
//        WeightInitializer initializer = new WeightInitializerGaussianFixedVariance(1.0/784.0);

//        NNLayer[] layers = new NNLayer[3];
//        int numConvFilters = 100;
//        layers[0] = new NNLayerConv2D(14, 14, 1,
//                activation, initializer,
//                numConvFilters, // num filters
//                10, 10, 1, 2, // width, height, depth, stride
//                1, 0.0, // padding, paddingValue
//                learningRate, l2
//                );
//        int numConvOutputs = ((NNLayerConv2D)layers[0]).getNumOutputs();
//        System.out.println("Num Conv outputs: " + numConvOutputs);
//        layers[1] = new NNLayerMaxPooling(numConvOutputs, numConvOutputs/numConvFilters);
//        layers[2] = new NNLayerFullyConnected(numConvFilters, 10,
//                activation, initializer,
//                learningRate, l2
//                );
//        LayeredNN nn = new LayeredNN(layers);

        LayeredNN nn = LayeredNN.buildFullyConnected(new int[] {196, 200, NUM_OUTPUTS},
                activation,
                initializer,
                learningRate, l2
                );
        return new MNISTNNPredictor(nn);
    }

    public MNISTNNPredictor(LayeredNN nn) {
        this.nn = nn;
    }

    @Override
    public int predict(double[][] image) {
        double[] dist = this.predictDist(image);
        double max = dist[0];
        int maxIdx = 0;
        for (int i=1; i<dist.length; i++) {
            double current = dist[i];
            if (current > max) {
                max = current;
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    public double[] predictDist(double[][] image) {
        double[] features = createFeatures(image);
        return nn.activate(features);
    }

    private double[] createFeatures(double[][] image) {
        double[] features = new double[image.length * image[0].length];
        for (int i=0; i<image.length; i++) {
            for (int j=0; j<image[i].length; j++) {
                features[i*j + j] = image[i][j];
            }
        }
        return features;
    }

    public double[] logError(double[] preds, int label) {
        double[] probDist = toProbDist(preds);
        double[] errors = new double[preds.length];
        for (int i=0; i<errors.length; i++) {
            errors[i] = i == label
                    ? -Math.log(probDist[i])
                    : Math.log(1.0 - probDist[i]);
        }
        return errors;
    }

    private double[] logErrorDerivative(double[] preds, int label) {
        double[] probDist = toProbDist(preds);
        double[] errors = new double[preds.length];
        for (int i=0; i<errors.length; i++) {
            errors[i] = i == label
                    ? 1.0 / probDist[i]
                    : -1.0 / (1.0 - probDist[i]);
        }
        return errors;
    }

    private double[] squaredErrorDerivative(double[] preds, int label) {
        double[] errors = new double[preds.length];
        for (int i=0; i<errors.length; i++) {
            errors[i] = i == label
                    ? 1.0 - preds[i]
                    : -1.0 - preds[i];
        }
        return errors;
    }

    private double[] toProbDist(double[] preds) {
        double sum = 0;
        double[] probs = new double[preds.length];
        for (int i=0; i<preds.length; i++) {
            probs[i] = (preds[i] + 1.0) / 2.0; // convert from tanh to 0-1
            sum += probs[i];
        }
        for (int i=0; i<probs.length; i++) {
            probs[i] /= sum;
        }
        return probs;
    }

    private double[] correctOutputsForLabel(int label) {
        double[] correctOutput = new double[NUM_OUTPUTS];
        Arrays.fill(correctOutput, -1.0);
        correctOutput[label] = 1.0;
        return correctOutput;
    }

    public void update(double[] preds, int label) {
        // TODO squared error is probably not the best
        double[] errorGradient = squaredErrorDerivative(preds, label);
        nn.backprop(errorGradient);
    }

    public static List<LabeledImage> preprocessData(List<LabeledImage> trainingData) {
        ImageUtils.downsample14By14(trainingData);
        return trainingData;
    }

}
