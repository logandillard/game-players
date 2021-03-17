package com.dillard.nn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NNLayerResidual implements Serializable, NNLayer {
    private static final long serialVersionUID = 1L;
    private NNLayer[] layers;
    private double[] outputValues;
    private LayerBatchActivation batchActivation;
    private ActivationFunction activationFunction;

    public NNLayerResidual(NNLayer[] layers) {
        this.layers = layers;
        // We sneakily set a linear activation function because we will actually perform the last activation here
        NNLayer lastLayer = layers[layers.length - 1];
        this.activationFunction = lastLayer.getActivationFunction();
        lastLayer.setActivationFunction(new ActivationFunctionLinear());
    }

    @Override
    public NNLayerResidual clone() {
        NNLayer[] l = new NNLayer[this.layers.length];
        for (int i=0; i<l.length; i++) {
            l[i] = layers[i].clone();
        }
        l[l.length - 1].setActivationFunction(activationFunction);
        return new NNLayerResidual(l);
    }

    public NNLayerResidual cloneWeights() {
        NNLayer[] l = new NNLayer[this.layers.length];
        for (int i=0; i<l.length; i++) {
            l[i] = layers[i].cloneWeights();
        }
        l[l.length - 1].setActivationFunction(activationFunction);
        return new NNLayerResidual(l);
    }

    public double[] activate(double[] inputValues) {
        double[] outputValues = inputValues;
        for (NNLayer layer : layers) {
            outputValues = layer.activate(outputValues);
        }

        // We adopt the second nonlinearity after the addition
        for (int i=0; i<outputValues.length; i++) {
            outputValues[i] = activationFunction.activate(outputValues[i] + inputValues[i]);
        }
        this.outputValues = outputValues;
        return outputValues;
    }

    public double[] backprop(double[] errorGradient) {
        // We need to do this here because we trick the activation function on the last layer
        double[] outputDerivative = new double[errorGradient.length];
        for (int output=0; output<errorGradient.length; output++) {
            outputDerivative[output] = errorGradient[output] * activationFunction.derivative(outputValues[output]);
        }

        double[] nextLayerErrorGradient = outputDerivative;
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            nextLayerErrorGradient = layers[layer].backprop(nextLayerErrorGradient);
        }

        // pass the output error gradients back directly because weights are effectively 1.0
        for (int i=0; i<nextLayerErrorGradient.length; i++) {
            nextLayerErrorGradient[i] += outputDerivative[i];
        }

        return nextLayerErrorGradient;
    }

//    public double[] accumulateGradients(double[] errorGradient) {
//        // We need to do this here because we trick the activation function
//        double[] outputDerivative = new double[errorGradient.length];
//        for (int output=0; output<errorGradient.length; output++) {
//            outputDerivative[output] = errorGradient[output] * activationFunction.derivative(outputValues[output]);
//        }
//
//        double[] nextLayerErrorGradient = errorGradient;
//        for (int layer = layers.length - 1; layer >= 0; layer--) {
//            nextLayerErrorGradient = layers[layer].accumulateGradients(nextLayerErrorGradient);
//        }
//
//        // pass the output error gradients back directly because weights are effectively 1.0
//        for (int i=0; i<nextLayerErrorGradient.length; i++) {
//            nextLayerErrorGradient[i] += outputDerivative[i];
//        }
//
//        return nextLayerErrorGradient;
//    }
//
//    public void applyAccumulatedGradients() {
//        for (int layer = layers.length - 1; layer >= 0; layer--) {
//            layers[layer].applyAccumulatedGradients();
//        }
//    }

    public void setLearningRate(double lr) {
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            layers[layer].setLearningRate(lr);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (NNLayer layer : layers) {
            sb.append(layer.toString() + "\n");
        }
        return sb.toString();
    }

    public NNLayer[] getLayers() {
        return layers;
    }

    @Override
    public ActivationFunction getActivationFunction() {
        return activationFunction;
    }

    @Override
    public void setActivationFunction(ActivationFunction activationFunction) {
        this.activationFunction = activationFunction;
    }

    @Override
    public List<double[]> batchActivate(List<double[]> batchInputs) {
        List<double[]> batchOutputs = batchInputs;

        for (NNLayer layer : layers) {
            batchOutputs = layer.batchActivate(batchOutputs);
        }

        // We adopt the second nonlinearity after the addition
        for (int instanceIndex=0; instanceIndex<batchInputs.size(); instanceIndex++) {
            double[] inputValues = batchInputs.get(instanceIndex);
            double[] outputValues = batchOutputs.get(instanceIndex);
            for (int i=0; i<outputValues.length; i++) {
                outputValues[i] = activationFunction.activate(outputValues[i] + inputValues[i]);
            }
        }

        this.batchActivation = new LayerBatchActivation(batchInputs, batchOutputs, null, null, null, null);
        return batchOutputs;
    }

    @Override
    public List<double[]> batchBackprop(List<double[]> batchErrorGradients) {
        List<double[]> batchNextLayerErrorGradient;
        List<double[]> batchOutputDerivative = new ArrayList<>(batchErrorGradients.size());
        for (int instanceIndex=0; instanceIndex<batchErrorGradients.size(); instanceIndex++) {
            double[] errorGradient = batchErrorGradients.get(instanceIndex);

            // We need to do this here because we trick the activation function on the last layer
            double[] outputDerivative = new double[errorGradient.length];
            for (int output=0; output<errorGradient.length; output++) {
                outputDerivative[output] = errorGradient[output] * activationFunction.derivative(
                        batchActivation.outputs.get(instanceIndex)[output]);
            }
            batchOutputDerivative.add(outputDerivative);
        }

        // work backwards through our layers
        batchNextLayerErrorGradient = batchOutputDerivative;
        for (int layer = layers.length - 1; layer >= 0; layer--) {
            batchNextLayerErrorGradient = layers[layer].batchBackprop(batchNextLayerErrorGradient);
        }

        for (int instanceIndex=0; instanceIndex<batchNextLayerErrorGradient.size(); instanceIndex++) {
            double[] outputDerivative = batchOutputDerivative.get(instanceIndex);
            double[] nextLayerErrorGradient = batchNextLayerErrorGradient.get(instanceIndex);
            // pass the output error gradients back directly because weights are effectively 1.0
            for (int i=0; i<outputDerivative.length; i++) {
                nextLayerErrorGradient[i] += outputDerivative[i];
            }
        }

        return batchNextLayerErrorGradient;
    }
}
