package com.dillard.nn;

public interface ActivationFunction {

    double activate(double x);
    
    double derivative(double value);

}
