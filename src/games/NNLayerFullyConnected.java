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
    private final ActivationFunction activationFunction;
    
    public NNLayerFullyConnected(int numInputs, int numOutputs, 
            ActivationFunction activationFunction, 
            WeightInitializer initializer,
            double learningRate, double eligDecay, 
            double l2Regularization, double l1Regularization) {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.activationFunction = activationFunction;
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
    
    public void updateElig() {
        double[] outputDerivative = new double[numOutputs];
        for (int output=0; output<numOutputs; output++) {
            outputDerivative[output] = activationFunction.derivative(outputValues[output]);
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
