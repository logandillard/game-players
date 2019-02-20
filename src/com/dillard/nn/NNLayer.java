package com.dillard.nn;

public interface NNLayer extends Cloneable {

    double[] activate(double[] inputValues);

    double[] backprop(double[] errorGradient);

    NNLayer clone();
}