package com.dillard.nn;

import java.util.List;

public interface NNLayer extends Cloneable {

    double[] activate(double[] inputValues);

    double[] backprop(double[] errorGradient);

    NNLayer clone();

//    double[] accumulateGradients(double[] errorGradients);
//
//    void applyAccumulatedGradients();

    NNLayer cloneWeights();

    void setLearningRate(double lr);

    ActivationFunction getActivationFunction();

    void setActivationFunction(ActivationFunction activationFunction);

    List<double[]> batchActivate(List<double[]> batchInputs);

    List<double[]> batchBackprop(List<double[]> batchErrorGradients);
}