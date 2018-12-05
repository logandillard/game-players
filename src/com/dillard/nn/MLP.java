package com.dillard.nn;

import java.io.Serializable;
import java.util.Random;

// Multi-layered perceptron
public class MLP implements Serializable {
	static final long serialVersionUID = 1;
	static final boolean SIGMOID_OUTPUT = false;
	static final boolean USE_BIAS = true;
	static final double BIAS_VALUE = -1.0;
	static final double INITIAL_WEIGHT_RANGE = 0.1;
	private static final double convergenceThreshold = 0.01;
	private int numHiddenLayers;
	private int numInputUnits;
	private int numOutputUnits;
	private Random rand;
	private boolean useRandom;
	private double initialWeight;

	double alpha, beta, gamma, lambda;

	private double[][] inputWeights; // Weights from inputs to first hidden layer
	private double[][][] eligTraceInput;
	private double[][][] hiddenWeights;
	private double[][][] eligTraceHidden;
	private double[][] activationLevels;
	private double[] outputs;
	//private double rawOutput;
	private double[] inputs;


	// Constructor with random initial weights
	public MLP(int[] numUnitsByLayer) {
		useRandom = true;
		rand = new Random();
		initialize(numUnitsByLayer);
	}
	// Constructor specifying a default initial weight
	public MLP(int[] numUnitsByLayer, double initialWeight) {
		useRandom = false;
		this.initialWeight = initialWeight;
		initialize(numUnitsByLayer);
	}


	// Constructor helper method
	private void initialize(int[] numUnitsByLayer) {

		this.numHiddenLayers = numUnitsByLayer.length - 2;
		this.numInputUnits = numUnitsByLayer[0];
		this.numOutputUnits = numUnitsByLayer[numUnitsByLayer.length - 1];

		if(USE_BIAS) numInputUnits++;

		inputs = null;

		hiddenWeights = new double[numHiddenLayers][][];
		eligTraceHidden = new double[numHiddenLayers][][];
		activationLevels = new double[numHiddenLayers + 1][];
		int nextLayerSize;
		for(int i=0; i<numHiddenLayers; i++) {
			nextLayerSize = (i+1 >= numHiddenLayers ? numOutputUnits : numUnitsByLayer[i+1]);
			hiddenWeights[i] = new double[numUnitsByLayer[i + 1]][nextLayerSize];
			eligTraceHidden[i] = new double[numUnitsByLayer[i + 1]][nextLayerSize];
			activationLevels[i] = new double[numUnitsByLayer[i + 1]];
		}
		activationLevels[numHiddenLayers] = new double[numOutputUnits];
		outputs = activationLevels[numHiddenLayers];

		int layer1Size = (numHiddenLayers > 0 ? numUnitsByLayer[1] : numOutputUnits);
		inputWeights = new double[numInputUnits][layer1Size];
		if(numHiddenLayers != 1) throw new UnsupportedOperationException(
				"Configured for 1 hidden layer only!");
		eligTraceInput = new double[numInputUnits][layer1Size][numOutputUnits];

		// Set all weights to default
		for(int i=0; i<inputWeights.length; i++) {
			for(int j=0; j<inputWeights[i].length; j++) {
				inputWeights[i][j] = getNextInitialWeight();
			}
		}
		for(int i=0; i<hiddenWeights.length; i++) {
			for(int j=0; j<hiddenWeights[i].length; j++) {
				for(int k=0; k<hiddenWeights[i][j].length; k++) {
					hiddenWeights[i][j][k] = getNextInitialWeight();
				}
			}
		}
	}


	public void initEligTraces() {
		int numHiddenUnits = inputWeights[0].length;
		eligTraceInput = new double[numInputUnits][numHiddenUnits][numOutputUnits];
		eligTraceHidden = new double[numHiddenLayers][numHiddenUnits][numOutputUnits];
	}


	// The method to set the network into action
	public double[] activate(double[] inputValues) throws IncorrectInputsException{
		if(!USE_BIAS) {
			if(inputValues.length != numInputUnits)
				throw new IncorrectInputsException();

			this.inputs = inputValues;
		}
		else { // Use the bias
			if(inputValues.length + 1 != numInputUnits)
				throw new IncorrectInputsException();

			this.inputs = inputValues;
			// Add in the bias to the input values
			// ***** This is really slow to copy the array of doubles every time!
			this.inputs = new double[inputValues.length + 1];
			for(int i=0; i<inputValues.length; i++) this.inputs[i] = inputValues[i];
			this.inputs[this.inputs.length - 1] = BIAS_VALUE;
		}


		clearActivationLevels();
		double tempActivationLevel;

		// For each input value
		for(int i=0; i<inputs.length; i++) {

			tempActivationLevel = inputs[i];

			if(tempActivationLevel != 0.0) {
				// For each unit in the first hidden layer
				for(int j=0; j<inputWeights[i].length; j++) {
					// Add to the value of the inputs for that unit
					activationLevels[0][j] +=  tempActivationLevel * inputWeights[i][j];
				}
			}
		}

//		// Add the bias
//		// For each unit in the first hidden layer
//		for(int j=0; j<inputWeights[inputWeights.length - 1].length; j++) {
//			// Add to the value of the inputs for that unit
//			activationLevels[0][j] +=  BIAS_VALUE * inputWeights[inputWeights.length - 1][j];
//		}

		// Apply the activation function to the units in the first hidden layer
		for(int j=0; j<activationLevels[0].length; j++){
			activationLevels[0][j] = sigmoid(activationLevels[0][j]);
		}


		// Set the activation values of all other hidden layers
		// For each hidden link layer (including the output layer)
		for(int i=1; i<activationLevels.length; i++) {
			// For each unit in the layer
			for(int j=0; j<hiddenWeights[i-1].length; j++) {
				// For each link the unit has

				tempActivationLevel = activationLevels[i-1][j];

				//if(tempActivationLevel != 0.0)
				for(int k=0; k<hiddenWeights[i-1][j].length; k++) {
					// Add to the activation value
					activationLevels[i][k] += tempActivationLevel * hiddenWeights[i-1][j][k];
				}
			}

			if(activationLevels[i] != outputs) {
				// Apply the activation function to the units if this is not the output layer
				for(int j=0; j<activationLevels[i].length; j++){
					activationLevels[i][j] = sigmoid(activationLevels[i][j]);
				}
			}
		}
//		if(Double.NaN == outputs[0]) {
//			throw new ModelBlewUpException("Output = " + outputs[0]);
//		}


		//**********fudged**************
		//rawOutput = outputs[0];

		//******************************
		if(SIGMOID_OUTPUT) {
			for(int i=0; i<outputs.length; i++) outputs[i] = activationFunction(outputs[i]);
		}
		//********************

		return outputs;
	}




	// LEARNING-RELATED METHODS


	public void TDLearn(double error) {
		if(error == 0.0) return;

		double alphaError = alpha * error;
		double betaError = beta * error;

		// For each of the output units
		for(int k=0; k < numOutputUnits; k++) {
			// For each of the hidden units
			for(int j=0; j < activationLevels[0].length; j++) {

				// For each input unit
				for(int i=0; i<numInputUnits; i++) {
					// Set the input to hidden layer elig traces
					inputWeights[i][j] += (alphaError * eligTraceInput[i][j][k]);
				}

				// Set the hidden to output layer weights
				hiddenWeights[0][j][k] += (betaError * eligTraceHidden[0][j][k]);
			}
		}
	}


	public void updateElig() {
		if(numHiddenLayers > 1) throw new UnsupportedOperationException(
				"Only configured for one hidden layer! Sorry! Can be updated, I'm sure.");
		if(inputs == null) throw new IllegalArgumentException("No inputs have been set!");

		// Calculate the derivative of the outputs
		double[] derivOutputs = new double[numOutputUnits];
		for(int k=0; k<numOutputUnits; k++) {
			if(SIGMOID_OUTPUT) derivOutputs[k] = (outputs[k] * (1 - outputs[k]));
			else derivOutputs[k] = 1.0; // /(double)hiddenWeights[0].length;
		}

		double temp;

		// For each of the hidden units
		for(int j=0; j < activationLevels[0].length; j++) {
			// For each of the output units
			for(int k=0; k < numOutputUnits; k++) {
				// Set the hidden to output layer elig traces
				eligTraceHidden[0][j][k] =  (lambda * eligTraceHidden[0][j][k]) +
					derivOutputs[k] * activationLevels[0][j];

				temp = derivOutputs[k] * hiddenWeights[0][j][k]
                     * activationLevels[0][j] * (1-activationLevels[0][j]);

				// For each input unit
				for(int i=0; i<numInputUnits; i++) {
					// Set the input to hidden layer elig traces
					eligTraceInput[i][j][k] = (lambda * eligTraceInput[i][j][k]) +
						(temp * inputs[i]);
				}
			}
		}
	}


	public void setLearningParams(double alpha, double beta, double gamma, double lambda) {
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
		this.lambda = lambda;
	}

	public void backprop(double error) {
		if(outputs.length > 1) throw new RuntimeException("Written for just one output unit!");

		double deltaI;
		double deltaOut = error * (SIGMOID_OUTPUT ?
				outputs[0] * (1-outputs[0]): 1);

		for(int i=0; i<hiddenWeights[0].length; i++) {

			deltaI = deltaOut * hiddenWeights[0][i][0] *
				(activationLevels[0][i] * (1-activationLevels[0][i])); // times the gradient too!!


			for(int j=0; j<inputWeights.length; j++) {
				inputWeights[j][i] += inputs[j] * alpha * deltaI;
			}

			hiddenWeights[0][i][0] += (beta * deltaOut * activationLevels[0][i]);
		}
	}


	// UTILITY METHODS

	private void clearActivationLevels() {
		for(int i=0; i<activationLevels.length; i++) {
			for(int j=0; j<activationLevels[i].length; j++) {
				activationLevels[i][j] = 0.0;
			}
		}
	}

	private double getNextInitialWeight() {
		if(useRandom) return ((rand.nextDouble()* INITIAL_WEIGHT_RANGE) - (INITIAL_WEIGHT_RANGE/2.0));
				//(1.0/numInputUnits) - (0.5/numInputUnits));
		else return initialWeight;
	}

	// Retrieve the outputs of the network
	public double[] getOutputs() {
		return outputs;
	}

//	public double getRawOutput() {
//		return rawOutput;
//	}

	public double getHighestWeight() {
		double highestWeight = 0.0;

		for(int i=0; i<hiddenWeights.length; i++) {
			for(int j=0; j<hiddenWeights[i].length; j++) {
				for(int k=0; k<hiddenWeights[i][j].length; k++) {
					if(Math.abs(hiddenWeights[i][j][k]) > Math.abs(highestWeight)) {
						highestWeight = hiddenWeights[i][j][k];
					}
				}
			}
		}
		for(int i=0; i<inputWeights.length; i++) {
			for(int j=0; j<inputWeights[i].length; j++) {
				if(Math.abs(inputWeights[i][j]) > Math.abs(highestWeight)) {
					highestWeight = inputWeights[i][j];
				}
			}
		}

		return highestWeight;
	}

	private double activationFunction(double sumInputs) {
		//return sumInputs;
		return sigmoid(sumInputs);
	}

	private double sigmoid(double x) {
		return 1.0 / (1.0 + Math.pow(Math.E, -x));
	}

//	private double sigmoidDeriv(double x) {
//		double s = sigmoid(x);
//		return s * (1.0 - s);
//	}


	@Override
    public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("Hidden Weights:\n");
		for(int i=0; i< hiddenWeights[0].length; i++) {
			for(int j=0; j< outputs.length; j++) {
				sb.append(hiddenWeights[0][i][j] + " | ");
			}
			sb.append(" || ");
		}

		sb.append("\nInput Weights:\n");
		for(int i=0; i<numInputUnits; i++) {
			for(int j=0; j<hiddenWeights[0].length; j++) {
				sb.append( " | " + inputWeights[i][j]);
			}
			sb.append(" || ");
		}

		sb.append("\n");



		return sb.toString();
	}

	// Test that activate functions to some extent
//	public static void main(String args[]) {
//		MLP mlp = new MLP(1, 4, new int[] {2}, 5, 0.05);
//
//		double[] ins = new double[] {0.7, 0.3, 0.5, 0.1};
//		double[] outs = null;
//
//		try{
//			outs = mlp.activate(ins);
//		} catch(IncorrectInputsException exc) {
//			throw new RuntimeException();
//		}
//
//		StringBuffer sb = new StringBuffer();
//		sb.append("ins: ");
//		for(double d: ins) sb.append(d + " ");
//		System.out.println(sb.toString());
//
//		sb = new StringBuffer();
//		sb.append("outs: ");
//		for(double d: outs) sb.append(d + " ");
//		System.out.println(sb.toString());
//	}
}
