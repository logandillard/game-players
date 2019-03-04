package com.dillard.nn;

import java.io.Serializable;

public class ActivationFunctionLinear implements ActivationFunction, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public final double derivative(double value) {
        return 1.0;
    }

    @Override
    public final double activate(double x) {
        return x;
    }
}
