package com.dillard.nn;

import java.io.Serializable;

public class ActivationFunctionTanH implements ActivationFunction, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public final double derivative(double value) {
        return 1.0 - value*value;
         // Alternatively:
//        double modifiedOutput = (value + 1.0) * 0.5; // transform to sigmoid-equivalent
//        double activationDerivative = 2.0 * modifiedOutput * (1 - modifiedOutput); // * 2 for tanh
//        return activationDerivative;
    }

    @Override
    public final double activate(double x) {
        // Alternatively:
        // double ex = Math.exp(x);
        // double enx = Math.exp(-x);
        // return (ex - enx) / (ex + enx);
        return (2.0 / (1.0 + Math.exp(-x))) - 1.0;
    }
}
