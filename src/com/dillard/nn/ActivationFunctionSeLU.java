package com.dillard.nn;

import java.io.Serializable;

public class ActivationFunctionSeLU implements ActivationFunction, Serializable {
    // https://mlfromscratch.com/activation-functions-explained/#selu
    private static final long serialVersionUID = 1L;
    private static final double alpha = 1.6732632423543772848170429916717;
    private static final double lambda = 1.0507009873554804934193349852946;

    @Override
    public final double derivative(double value) {
        return value > 0 ?
                lambda :
                lambda * alpha * Math.exp(value);
    }

    @Override
    public final double activate(double x) {
        return x > 0 ?
                lambda*x :
                alpha * Math.exp(x) - alpha;
    }
}
