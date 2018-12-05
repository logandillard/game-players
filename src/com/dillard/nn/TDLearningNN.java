package com.dillard.nn;

public interface TDLearningNN {

    void reset(); 
    
    double[] activate(double[] inputValues);
    
    void updateElig();
    
    void tdLearn(double[] errors);
}