package com.dillard.mnist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MNISTTrainer {

    public MNISTPredictor trainPredictor(List<LabeledImage> trainingData) {
        MNISTNNPredictor predictor = MNISTNNPredictor.build();

        final int maxIters = 50;
        final int toleranceIters = 4;
        final double tolerance = 0.01;
        double max = -1.0;
        Random rand = new Random(23498);
        List<Double> maxList = new ArrayList<>();
        for (int i=0; i<maxIters; i++) {
            Collections.shuffle(trainingData, rand);
            trainOneIteration(trainingData, predictor);
            double accuracy = accuracy(trainingData, predictor);
            System.out.println(String.format("%02d  %.4f  - %s", i, accuracy, new Date()));

            if (accuracy > max) {
                max = accuracy;
            }

            maxList.add(max);

            if (maxList.size() > toleranceIters) {
                double ratio = (1.0 - max) / (1.0 - maxList.get(i - toleranceIters));
                if (ratio > 1 - tolerance) {
                    System.out.println("Converged");
                    break;
                }
            }
        }

        return predictor;
    }

    private void trainOneIteration(List<LabeledImage> trainingData, MNISTNNPredictor nn) {
        for (LabeledImage li : trainingData) {
            double[] preds = nn.predictDist(li.image);
            nn.update(preds, li.label);
        }
    }

    private double sum(double[] a) {
        double sum = 0;
        for (double d : a) {
            sum += d;
        }
        return sum;
    }

    public MNISTPredictor baselinePredictor() {
        return image -> 0;
    }

    public double evaluatePredictor(List<LabeledImage> testData, MNISTPredictor predictor) {
        return accuracy(testData, predictor);
    }

    private double accuracy(List<LabeledImage> testData, MNISTPredictor predictor) {
        int numImages = 0, numCorrect = 0;
        for (int i=0; i<testData.size(); i++) {
            LabeledImage labeledImage = testData.get(i);

            int prediction = predictor.predict(labeledImage.image);
            if (prediction == labeledImage.label) {
                numCorrect++;
            }
            numImages++;
        }
        return numCorrect / (double)numImages;
    }

    public static List<LabeledImage> preprocessData(List<LabeledImage> trainingData) {
        return MNISTNNPredictor.preprocessData(trainingData);
    }
}
