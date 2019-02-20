package com.dillard.mnist;

import java.util.List;
import java.util.Random;

import com.dillard.nn.ActivationFunction;
import com.dillard.nn.ActivationFunctionTanH;
import com.dillard.nn.LayeredNN;
import com.dillard.nn.WeightInitializer;
import com.dillard.nn.WeightInitializerGaussianFixedVariance;

public class Benchmark {
    public static void main(String[] args) {
        String dataDir = args[0];
        List<int[][]> trainImages = MnistReader.getImages(dataDir + "/train-images-idx3-ubyte");
        int[] trainLabels = MnistReader.getLabels(dataDir + "/train-labels-idx1-ubyte");
        List<LabeledImage> trainingData = LabeledImage.build(trainImages, trainLabels);
        trainingData = MNISTTrainer.preprocessData(trainingData);
        System.out.println("Loaded data");


        double learningRate = 0.001, l2 = 0.0001;
        ActivationFunction activation = new ActivationFunctionTanH();
        WeightInitializer initializer = new WeightInitializerGaussianFixedVariance(1.0/196.0, new Random(1l));
        LayeredNN nn = LayeredNN.buildFullyConnected(new int[] {196, 100, 10},
                activation,
                initializer,
                learningRate, l2
                );
        MNISTNNPredictor predictor = new MNISTNNPredictor(nn);

        // warm up
        for (int i=0; i<1000; i++) {
            LabeledImage li = trainingData.get(i);
            double[] preds = predictor.predictDist(li.image);
            predictor.update(preds, li.label);
        }
        System.out.println("Warmed up");

        long start = System.currentTimeMillis();
        for (int i=0; i<5000; i++) {
            LabeledImage li = trainingData.get(i);
            double[] preds = predictor.predictDist(li.image);
            predictor.update(preds, li.label);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
}
