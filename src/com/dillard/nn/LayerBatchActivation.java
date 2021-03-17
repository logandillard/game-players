package com.dillard.nn;

import java.io.Serializable;
import java.util.List;

public class LayerBatchActivation implements Serializable {
    private static final long serialVersionUID = 1L;
    public final List<double[]> inputs;
    public final List<double[]> outputs;
    public final double[] means;
    public final double[] variances;
    public final double[] inverseStdDev;
    public final List<double[]> batchNormalizedH;

    public LayerBatchActivation(List<double[]> batchInputs, List<double[]> batchOutputs, double[] means,
            double[] variances, double[] inverseStdDev, List<double[]> batchNormalizedH) {
        this.inputs = batchInputs;
        this.outputs = batchOutputs;
        this.means = means;
        this.variances = variances;
        this.inverseStdDev = inverseStdDev;
        this.batchNormalizedH = batchNormalizedH;
    }
}
