package com.dillard.mnist;

import java.util.List;

public class MNISTApp {
    public static void main(String[] args) {
        String dataDir = args[0];
        List<int[][]> trainImages = MnistReader.getImages(dataDir + "/train-images-idx3-ubyte");
        int[] trainLabels = MnistReader.getLabels(dataDir + "/train-labels-idx1-ubyte");
        List<LabeledImage> trainingData = LabeledImage.build(trainImages, trainLabels);
        trainingData = MNISTTrainer.preprocessData(trainingData);
        System.out.println("Loaded data");

        MNISTTrainer trainer = new MNISTTrainer();
        System.out.println("Training");
        MNISTPredictor predictor = trainer.trainPredictor(trainingData);
        System.out.println("Finished training");

        List<int[][]> testImages = MnistReader.getImages(dataDir + "/t10k-images-idx3-ubyte");
        int[] testLabels = MnistReader.getLabels(dataDir + "/t10k-labels-idx1-ubyte");
        List<LabeledImage> testData = LabeledImage.build(testImages, testLabels);
        testData = MNISTTrainer.preprocessData(testData);

        double result = trainer.evaluatePredictor(testData, predictor);
        System.out.println("Evaluation result: " + result);
    }
}
