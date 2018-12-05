package com.dillard.nn;

public interface NNLayer {

    double[] activate(double[] inputValues);

    double[] backprop(double[] errorGradient);

}