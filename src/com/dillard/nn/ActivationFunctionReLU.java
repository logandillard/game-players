package com.dillard.nn;

import java.io.Serializable;

public class ActivationFunctionReLU implements ActivationFunction, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public final double derivative(double value) {
        return value > 0 ? 1.0 : 0.0;
    }

    @Override
    public final double activate(double x) {
        return x > 0 ? x : 0;
    }
}
