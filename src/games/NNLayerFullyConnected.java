package games;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

public class NNLayerFullyConnected implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int numInputs;
    private final int numOutputs;
    private double[][] weights;
    private double[][] eligTraces;
    private double[] inputValues;
    private double[] outputValues;
    private double learningRate;
    private double eligDecay;
    private double l2Regularization;
    private double l1Regularization;
    private Random rand;
    private final double beta1 = 0.9;
    private final double beta2 = 0.999;
    private double beta1t = beta1;
    private double beta2t = beta2;
    private double[][] adamM;
    private double[][] adamV;
    static final double DEFAULT_INITIAL_WEIGHT_RANGE = 0.2;
    private double initialWeightRange = DEFAULT_INITIAL_WEIGHT_RANGE;

    public NNLayerFullyConnected(int numInputs, int numOutputs, double learningRate, double eligDecay, 
            double l2Regularization, double l1Regularization) {
        this(numInputs, numOutputs, learningRate, eligDecay, l2Regularization, l1Regularization, DEFAULT_INITIAL_WEIGHT_RANGE);
    }
    
    public NNLayerFullyConnected(int numInputs, int numOutputs, double learningRate, double eligDecay, 
            double l2Regularization, double l1Regularization, double initialWeightRange) {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.learningRate = learningRate;
        this.eligDecay = eligDecay;
        this.l2Regularization = l2Regularization;
        this.l1Regularization = l1Regularization;
        weights = new double[numInputs + 1][numOutputs]; // + 1 for biases
        eligTraces = new double[numInputs + 1][numOutputs]; // + 1 for biases
        adamM = new double[numInputs + 1][numOutputs]; // + 1 for biases
        adamV = new double[numInputs + 1][numOutputs]; // + 1 for biases
        outputValues = new double[numOutputs];
        
        // initialize weights randomly
        this.initialWeightRange = initialWeightRange;
        rand = new Random(System.currentTimeMillis());
        initializeWeights();
    }

    private void initializeWeights() {
        for (int input=0; input<weights.length; input++) {
            for (int output=0; output<weights[input].length; output++) {
                weights[input][output] = nextInitialWeight();
            }
        }
    }
    
    private double nextInitialWeight() {
        return ((rand.nextDouble()* initialWeightRange) - (initialWeightRange/2.0));
    }
    
    public void initEligTraces() {
        eligTraces = new double[numInputs + 1][numOutputs]; // + 1 for bias
    }
    
    public double[] activate(double[] inputValues) {
        this.inputValues = inputValues;
        Arrays.fill(outputValues, 0.0);
        for (int input=0; input<numInputs; input++) {
            if (inputValues[input] == 0.0) {
                continue;
            }
            for (int output=0; output<numOutputs; output++) {
                outputValues[output] += inputValues[input] * weights[input][output];
            }
        }
        // biases
        for (int output=0; output<numOutputs; output++) {
            outputValues[output] += weights[numInputs][output];
        }
        // link function
        for (int output=0; output<numOutputs; output++) {
            outputValues[output] = linkFunction(outputValues[output]);
        }
        return outputValues;
    }
    
    public double[] backprop(double[] errorGradient) {
        double[] inputNodeGradient = new double[numInputs];

        double[] outputDerivative = new double[numOutputs];
        for (int output=0; output<numOutputs; output++) {
            double modifiedOutput = (outputValues[output] + 1.0) / 2.0; // transform to sigmoid-equivalent
            double activationDerivative = 2.0 * modifiedOutput * (1 - modifiedOutput); // * 2 for tanh
            outputDerivative[output] = errorGradient[output] * activationDerivative;
        }
        
        for (int input=0; input<numInputs; input++) {
            for (int output=0; output<numOutputs; output++) {
                double gradient = inputValues[input] * outputDerivative[output];

                // ADAM update
                adamUpdateWeight(input, output, gradient);
                
                // regularization
                double currentWeight = weights[input][output];
                weights[input][output] += learningRate * (
                    - (currentWeight * l2Regularization)
                    - (l1Regularization * Math.signum(currentWeight)));
                
                // sum the derivative of the output with respect to the input for a previous layer to use
                inputNodeGradient[input] += outputDerivative[output] * currentWeight; // TODO update after regularization?
            }
        }
        
        // biases
        for (int output=0; output<numOutputs; output++) {
            // ADAM update
            adamUpdateWeight(numInputs, output, outputDerivative[output]);
            // no regularization on biases
        }
        
        beta1t *= beta1;
        beta2t *= beta2;
        
        return inputNodeGradient;
    }
    
    public void updateElig() {
        double[] outputDerivative = new double[numOutputs];
        for (int output=0; output<numOutputs; output++) {
            double modifiedOutput = (outputValues[output] + 1.0) / 2.0; // transform to sigmoid-equivalent
            outputDerivative[output] = 2.0 * modifiedOutput * (1 - modifiedOutput); // * 2 for tanh
        }
        
        for (int input=0; input<numInputs; input++) {
            for (int output=0; output<numOutputs; output++) {
                double gradient = inputValues[input] * outputDerivative[output];
                
                // SGD update
//                eligTraces[input][output] = (eligDecay * eligTraces[input][output]) + gradient;
                
                // ADAM update
                adamUpdateEligTraces(input, output, gradient);
            }
        }
        
        // biases
        for (int output=0; output<numOutputs; output++) {
            double gradient = outputDerivative[output];
            
            // SGD update
//            eligTraces[numInputs][output] = (eligDecay * eligTraces[numInputs][output]) + gradient;

            // ADAM update
            adamUpdateEligTraces(numInputs, output, gradient);
        }
        
        beta1t *= beta1;
        beta2t *= beta2;
    }
    
    private void adamUpdateEligTraces(int input, int output, double gradient) {
        // ADAM update
        adamM[input][output] = beta1 * adamM[input][output] + 
                (1.0 - beta1) * gradient;
        adamV[input][output] = beta2 * adamV[input][output] + 
                (1.0 - beta2) * gradient * gradient;
        
        double adjustedM = adamM[input][output] / (1.0 - beta1t);
        double adjustedV = adamV[input][output] / (1.0 - beta2t);
        
        eligTraces[input][output] = (eligDecay * eligTraces[input][output]) +
                (adjustedM / (Math.sqrt(adjustedV) + 0.00000001) );
    }
    
    private void adamUpdateWeight(int input, int output, double gradient) {
        // ADAM update
        adamM[input][output] = beta1 * adamM[input][output] + 
                (1.0 - beta1) * gradient;
        adamV[input][output] = beta2 * adamV[input][output] + 
                (1.0 - beta2) * gradient * gradient;
        
        double adjustedM = adamM[input][output] / (1.0 - beta1t);
        double adjustedV = adamV[input][output] / (1.0 - beta2t);
        
        weights[input][output] += learningRate * (adjustedM / (Math.sqrt(adjustedV) + 0.00000001));
    }
    
    public double[] tdLearn(double[] errors) {
        double[] inputNodeErrors = new double[numInputs + 1];
        for (int input=0; input<weights.length; input++) {
            for (int output=0; output<weights[input].length; output++) {
                inputNodeErrors[input] += errors[output] * weights[input][output];
                        
                weights[input][output] += errors[output] * learningRate * eligTraces[input][output] 
                        + learningRate * (-weights[input][output] * l2Regularization
                        - l1Regularization * Math.signum(weights[input][output]));
            }
        }
        return inputNodeErrors;
    }
    
    private double linkFunction(double x) {
        // tanh
        return (2.0 / (1.0 + Math.pow(Math.E, -x))) - 1.0;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int input=0; input<weights.length; input++) {
            sb.append(Arrays.toString(weights[input]) + "\n");
        }
        return sb.toString();
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public double getEligDecay() {
        return eligDecay;
    }

    public void setEligDecay(double eligDecay) {
        this.eligDecay = eligDecay;
    }

    public double getL2Regularization() {
        return l2Regularization;
    }

    public void setL2Regularization(double l2Regularization) {
        this.l2Regularization = l2Regularization;
    }

    public double getL1Regularization() {
        return l1Regularization;
    }

    public void setL1Regularization(double l1Regularization) {
        this.l1Regularization = l1Regularization;
    }
}
