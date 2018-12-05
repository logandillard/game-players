package games;

public interface NNLayer {

    double[] activate(double[] inputValues);

    double[] backprop(double[] errorGradient);

}