package com.dillard.mnist;

import java.util.ArrayList;
import java.util.List;

public class LabeledImage {
    public double[][] image;
    public int label;

    public LabeledImage(double[][] image, int label) {
        this.image = image;
        this.label = label;
    }

    public static List<LabeledImage> build(List<int[][]> images, int[] labels) {
        List<LabeledImage> lis = new ArrayList<>();
        for (int i=0; i<images.size(); i++) {
            lis.add(new LabeledImage(normalizePixelValues(images.get(i)), labels[i]));
        }
        return lis;
    }

    public static double[][] normalizePixelValues(int[][] image) {
        double[][] normalized = new double[image.length][image[0].length];
        for (int i=0; i<image.length; i++) {
            for (int j=0; j<image[i].length; j++) {
                normalized[i][j] = image[i][j] / MNISTConstants.MAX_PIXEL_VALUE;
            }
        }
        return normalized;
    }
}
