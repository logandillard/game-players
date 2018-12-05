package games;

public class ActivationFunctionTanH implements ActivationFunction {

    @Override
    public final double derivative(double value) {
        double modifiedOutput = (value + 1.0) / 2.0; // transform to sigmoid-equivalent
        double activationDerivative = 2.0 * modifiedOutput * (1 - modifiedOutput); // * 2 for tanh
        return activationDerivative;
    }

    @Override
    public final double activate(double x) {
        return (2.0 / (1.0 + Math.pow(Math.E, -x))) - 1.0;
    }
}
