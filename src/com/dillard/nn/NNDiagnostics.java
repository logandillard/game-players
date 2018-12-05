package com.dillard.nn;

import java.util.Arrays;

public class NNDiagnostics {
    public static String weightsToString(double[][] weights) {
        StringBuilder sb = new StringBuilder();
        for (int input=0; input<weights.length; input++) {
            sb.append(Arrays.toString(weights[input]) + "\n");
        }
        return sb.toString();
    }
}
