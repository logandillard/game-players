package com.dillard.nn;

public interface OptimizerMatrix {

    /** Not a clone, just a new one with the same parameters. Does not copy state. */
    OptimizerMatrix copyNew();

    void update(double[][] weights, int row, int col, double gradient, boolean applyRegularization);

//    double getUpdate(int row, int col, double gradient);
//
//    void applyUpdate(double update, double[][] weights, int row, int col, boolean applyRegularization);

    void incrementIteration();

    double getLrTimesL2();

    void setLearningRate(double lr);

}