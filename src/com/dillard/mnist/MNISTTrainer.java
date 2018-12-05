package com.dillard.mnist;

import java.util.List;

public class MNISTTrainer {

    public MNISTPredictor trainPredictor(List<LabeledImage> trainingData) {        
        MNISTNNPredictor nn = new MNISTNNPredictor();
        
        for (int i=0; i<40; i++) {
            trainOneIteration(trainingData, nn);
            System.out.println(String.format("%.4f", accuracy(trainingData, nn)));
        }
        
        return nn;
    }

    private void trainOneIteration(List<LabeledImage> trainingData, MNISTNNPredictor nn) {
        for (LabeledImage li : trainingData) {
            double[] preds = nn.predictDist(li.image);
            nn.update(preds, li.label);
            
            // TODO check results before and after
            double[] errorsBefore = nn.logError(preds, li.label);
            double errorBefore = sum(errorsBefore);
            double[] predsAfter = nn.predictDist(li.image);
            double[] errorsAfter = nn.logError(predsAfter, li.label);
            double errorAfter = sum(errorsAfter);
            
            @SuppressWarnings("unused")
            double relativeChange = errorAfter / errorBefore;
            @SuppressWarnings("unused")
            int f = 0;
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
        return new MNISTPredictor() {
            public int predict(double[][] image) {
                return 0;
            }
        };
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
