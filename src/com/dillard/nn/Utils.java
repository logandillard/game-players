package com.dillard.nn;

public class Utils {
    public static double[][] copyArray2D(double[][] input) {
        double [][] matrix = new double[input.length][];
        for(int i = 0; i < input.length; i++) {
            double[] a = input[i];
            int aLength = a.length;
            matrix[i] = new double[aLength];
            System.arraycopy(a, 0, matrix[i], 0, aLength);
        }
        return matrix;
    }
}
