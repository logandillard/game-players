package com.dillard.mnist;

import java.util.List;

public class ImageUtils {

    public static void downsample7By7(List<LabeledImage> images) {
        for (LabeledImage li : images) {
            li.image = downsample(li.image, 4);
        }
    }

    public static double[][] downsample(double[][] originalImage, int scaleFactor) {
        double[][] output = new double[originalImage.length / scaleFactor][originalImage[0].length / scaleFactor];
        for (int i=0; i<originalImage.length; i+=scaleFactor) {
            for (int j=0; j<originalImage[i].length; j+=scaleFactor) {
                double val = imageAverage(originalImage, i, j, scaleFactor);
                output[i/scaleFactor][j/scaleFactor] = val;
            }
        }
        return output;
    }
        
    public static double imageAverage(double[][] image, int row, int col, int scaleFactor) {
        double sum = 0;
        int count = 0;
        for (int i=row; i<row + scaleFactor; i++) {
            for (int j=col; j<col + scaleFactor; j++) {
                sum += image[i][j];
                count++;
            }
        }
        return sum / count;
    }
}
