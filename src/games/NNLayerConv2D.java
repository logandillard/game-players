package games;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

public class NNLayerConv2D implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int numInputs;
    private final int rowLength;
    private final int numFilters;
    private final int width;
    private final int height; 
    private final int stride; 
    private final int padding; 
    private final double paddingValue;
    private double[][] weights;
    private double[] inputValues;
    private double[] outputValues;
    private double learningRate;
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
    private final ActivationFunction activationFunction;

    public NNLayerConv2D(int numInputs, int rowLength, int numFilters, 
            ActivationFunction activationFunction, WeightInitializer initializer,
            int width, int height, int stride, int padding, double paddingValue, 
            double learningRate, double l2Regularization, double l1Regularization, double initialWeightRange) {
        this.numInputs = numInputs;
        this.rowLength = rowLength;
        this.numFilters = numFilters;
        this.activationFunction = activationFunction;
        
        //  (W − F + 2P)/ S + 1
        int inputHeight = numInputs / rowLength;
        int numOutputsPerFilterWidth = ((rowLength - width + 2*padding) / stride) + 1;
        int numOutputsPerFilterHeight = ((inputHeight - height + 2*padding) / stride) + 1;
        
        
        this.width = width;
        this.height = height;
        this.stride = stride;
        this.padding = padding;
        this.paddingValue = paddingValue;
        
        this.learningRate = learningRate;        
        this.l2Regularization = l2Regularization;
        this.l1Regularization = l1Regularization;
        
        weights = new double[numInputs + 1][numOutputs]; // + 1 for biases
        adamM = new double[numInputs + 1][numOutputs]; // + 1 for biases
        adamV = new double[numInputs + 1][numOutputs]; // + 1 for biases
        outputValues = new double[numOutputs];
        
        // initialize weights randomly
        this.initialWeightRange = initialWeightRange;
        rand = new Random(System.currentTimeMillis());
        initializeWeights(initializer);
    }

    private void initializeWeights(WeightInitializer initializer) {
        for (int input=0; input<weights.length; input++) {
            for (int output=0; output<weights[input].length; output++) {
                weights[input][output] = initializer.nextInitialWeight();
            }
        }
    }
    
    private double nextInitialWeight() {
        return ((rand.nextDouble()* initialWeightRange) - (initialWeightRange/2.0));
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
            outputValues[output] = activationFunction.activate(outputValues[output]);
        }
        return outputValues;
    }
    
    public double[] backprop(double[] errorGradient) {
        double[] inputNodeGradient = new double[numInputs];

        double[] outputDerivative = new double[numOutputs];
        for (int output=0; output<numOutputs; output++) {
            outputDerivative[output] = errorGradient[output] * activationFunction.derivative(outputValues[output]);
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
