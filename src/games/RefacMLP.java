package games;

import java.io.*;
import java.util.*;

import ioutilities.*;

// Multi-layered perceptron
public class RefacMLP implements Serializable, TDLearningNN {
	static final long serialVersionUID = 1;
	static final double MOMENTUM_FACTOR = .1;
	static final boolean SIGMOID_OUTPUT = false;
	static  boolean USE_BIAS = false;
	static  boolean USE_MOMENTUM = true;
	static final double BIAS_VALUE = -1.0;
	static final double INITIAL_WEIGHT_RANGE = 0.5;
	private static final double convergenceThreshold = 0.01;
	private int numHiddenLayers;
	private int numInputUnits;
	private int numOutputUnits;
	private int[] numUnitsByLayer;
	private Random rand;
	private boolean useRandomInitialWeights;
	
	private double initialWeight;
	private double learningRate, eligTraceDecayRate;
	
	private double[][][] weights;
	private double[][][] momentum;
	private double[][][] eligTraces;
	private double[][] activationLevels;
	
	
	// Constructor with random initial weights 
	public RefacMLP(int[] numUnitsByLayer) {
		useRandomInitialWeights = true;
		rand = new Random();
		initialize(numUnitsByLayer);
	}
	// Constructor specifying a default initial weight
	public RefacMLP(int[] numUnitsByLayer, double initialWeight) {
		useRandomInitialWeights = false;
		this.initialWeight = initialWeight;
		initialize(numUnitsByLayer);
	}
	
	
	// Constructor helper method
	private void initialize(int[] numUnitsByLayer) {
		if (numUnitsByLayer.length < 2) throw new RuntimeException(
				"Must have at least 2 layers. One for input and one for output.");
		
		this.numUnitsByLayer = numUnitsByLayer;
		this.numHiddenLayers = numUnitsByLayer.length - 2;
		this.numInputUnits = numUnitsByLayer[0];
		this.numOutputUnits = numUnitsByLayer[numUnitsByLayer.length - 1];
		
		if (numHiddenLayers != 1) throw new UnsupportedOperationException(
		"Configured for 1 hidden layer only!");

		if (USE_BIAS) {
			numInputUnits++;
			numUnitsByLayer[0]++;
		}
		
		//inputs = null;
		
		// Initialize weights, elig traces, and activation levels arrays
		weights = new double[numUnitsByLayer.length - 1][][];
		momentum = new double[numUnitsByLayer.length - 1][][];
		eligTraces = new double[numUnitsByLayer.length - 1][][];
		//****************************************
		
		activationLevels = new double[numUnitsByLayer.length][];
		
		int nextLayerSize;
		for(int i=0; i<numUnitsByLayer.length - 1; i++) {
			nextLayerSize = numUnitsByLayer[i+1];
			weights[i] = new double[numUnitsByLayer[i]][nextLayerSize];
			momentum[i] = new double[numUnitsByLayer[i]][nextLayerSize];
			eligTraces[i] = new double[numUnitsByLayer[i]][nextLayerSize];
			activationLevels[i] = new double[numUnitsByLayer[i]];
		}
		activationLevels[numUnitsByLayer.length - 1] = new double[numOutputUnits];
		
		// Set all weights to initial values (default or random)
		for(int i=0; i<weights.length; i++) {
			for(int j=0; j<weights[i].length; j++) {
				for(int k=0; k<weights[i][j].length; k++) {
					weights[i][j][k] = getNextInitialWeight();
				}
			}
		}
	}
	
	public void setLearningParams(double learningRate, double eligTraceDecayRate) {
		this.learningRate = learningRate;
		this.eligTraceDecayRate = eligTraceDecayRate;
	}
	
	/** Reset sequential information (use when beginning a new sequence) */
	public void reset() {
		initEligTraces();
		if(USE_MOMENTUM) initMomentum();
	}
	
	private void initEligTraces() {
		for(int i=0; i<numUnitsByLayer.length - 1; i++) {
			int nextLayerSize = numUnitsByLayer[i+1];
			eligTraces[i] = new double[numUnitsByLayer[i]][nextLayerSize];
		}
	}
	
	private void initMomentum() {
		for(int i=0; i<numHiddenLayers + 1; i++) {
			momentum[i] = new double[weights[i].length][weights[i][0].length];
		}
	}
	
	// The method to compute the outputs from the network
	public double[] activate(double[] inputValues) throws IncorrectInputsException{
		clearActivationLevels();
		
		if(!USE_BIAS) {
			if(inputValues.length != numInputUnits) throw new IncorrectInputsException();
			
			this.activationLevels[0] = inputValues;
		}
		else { // Use the bias
			if(inputValues.length + 1 != numInputUnits) throw new IncorrectInputsException();
			
			// Add in the bias to the input values
			// ***** This is really slow to copy the array of doubles every time!
			this.activationLevels[0] = new double[inputValues.length + 1];
			for(int i=0; i<inputValues.length; i++) this.activationLevels[0][i] = inputValues[i];
			this.activationLevels[0][this.activationLevels[0].length - 1] = BIAS_VALUE;
		}
		
		double tempActivationLevel;
		
		// For each layer before the last
		for(int baseLayer=0; baseLayer<=numHiddenLayers; baseLayer++) {
		
			// Sum the previous layer's values*weights
			// For each activation value in the previous layer
			for(int i=0; i<activationLevels[baseLayer].length; i++) {
				
				tempActivationLevel = activationLevels[baseLayer][i];
				
				if(tempActivationLevel != 0.0) {
					// For each unit in the next layer 
					for(int j=0; j<activationLevels[baseLayer + 1].length; j++) {
						// Add to the value of the inputs for that unit
						activationLevels[baseLayer + 1][j] +=  tempActivationLevel * weights[baseLayer][i][j]; 
					}
				}
			}
			
			// Apply the activation function (but not to the output layer unless SIGMOID_OUTPUT
			if((baseLayer < numHiddenLayers) || SIGMOID_OUTPUT) {
				for(int j=0; j<activationLevels[baseLayer + 1].length; j++){
					activationLevels[baseLayer + 1][j] = activationFunction(activationLevels[baseLayer + 1][j]);
				}
			}
		}
		
		
//		// Add the bias
//		// For each unit in the first hidden layer 
//		for(int j=0; j<inputWeights[inputWeights.length - 1].length; j++) {
//			// Add to the value of the inputs for that unit
//			activationLevels[0][j] +=  BIAS_VALUE * inputWeights[inputWeights.length - 1][j]; 
//		}
		

		return activationLevels[activationLevels.length - 1];
	}
	

	
	
	// LEARNING-RELATED METHODS
	
	public void tdLearn(double[] errors) {
		if (numHiddenLayers != 1) {
			throw new RuntimeException("Written for 1 hidden layer!");
		}
				
		// For each of the output units
		for(int k=0; k < numOutputUnits; k++) {
		    double alphaError = learningRate * errors[k];
			// For each of the hidden units
			for(int j=0; j < activationLevels[1].length; j++) {
				
				// Set the input to hidden layer weights
				for(int i=0; i<numInputUnits; i++) {
					double tmpDeltaWeight = (alphaError * eligTraces[0][i][j]);
					if (USE_MOMENTUM) tmpDeltaWeight += MOMENTUM_FACTOR * momentum[0][i][j];
					weights[0][i][j] += tmpDeltaWeight;
					if (USE_MOMENTUM) momentum[0][i][j] = tmpDeltaWeight;
				}
				
				// Set the hidden to output layer weights
				double tmpDeltaWeight = (alphaError * eligTraces[1][j][k]);
				if (USE_MOMENTUM) tmpDeltaWeight += MOMENTUM_FACTOR * momentum[1][j][k];
				weights[1][j][k] += tmpDeltaWeight;
				if (USE_MOMENTUM) momentum[1][j][k] = tmpDeltaWeight;
			}
		}
	}
	
	
	public void updateElig() {
		if(numHiddenLayers != 1) {
			throw new UnsupportedOperationException(
				"Only configured for one hidden layer! Sorry! Can be updated, I'm sure.");
		}
		
		// Calculate the derivative of the outputs
		double[] derivOutputs = new double[numOutputUnits];
		int outputLayer = activationLevels.length - 1;
		for (int k=0; k<numOutputUnits; k++) {
			if (SIGMOID_OUTPUT) derivOutputs[k] = (activationLevels[outputLayer][k] * (1 - activationLevels[outputLayer][k]));
			else derivOutputs[k] = 1.0;
		}
		
		// For each of the hidden units
		for(int j=0; j < activationLevels[1].length; j++) {
			
			// For each of the output units
			for(int output=0; output < numOutputUnits; output++) {
				// Set the hidden to output layer elig traces
			    eligTraces[1][j][output] = (eligTraceDecayRate * eligTraces[1][j][output]) +
					derivOutputs[output] * activationLevels[1][j];
				
				double temp = derivOutputs[output] * weights[1][j][output]  
                     * activationLevels[1][j] * (1-activationLevels[1][j]);
				
				// For each input unit
				for(int i=0; i<numInputUnits; i++) {
					// Set the input to hidden layer elig traces
				    eligTraces[0][i][j] = (eligTraceDecayRate * eligTraces[0][i][j]) +
						(temp * activationLevels[0][i]);                
				}
			}
		}
	}
	
	

	public void backprop(double error) {
		double[] outputs = activationLevels[activationLevels.length - 1];
		if(activationLevels[activationLevels.length - 1].length > 1) 
			throw new RuntimeException("Written for just one output unit!");
		
		double deltaI, aDeltaI, bDeltaOut, tmpDeltaWeight;
		double deltaOut = error * (SIGMOID_OUTPUT ? 
				outputs[0] * (1-outputs[0]): 1);
		bDeltaOut = deltaOut * learningRate;
		
		
		for (int i=0; i<weights[1].length; i++) {
			
			// Calculate intermediate values
			deltaI = deltaOut * weights[1][i][0] * 
				(activationLevels[1][i] * (1-activationLevels[1][i])); // times the gradient
			aDeltaI = deltaI * learningRate;
			
			// Do the input to hidden layer
			for(int j=0; j<weights[0].length; j++) {
				tmpDeltaWeight =  activationLevels[0][j] * aDeltaI;
				if(USE_MOMENTUM) tmpDeltaWeight += MOMENTUM_FACTOR * momentum[0][i][j];
				
				weights[0][j][i] += tmpDeltaWeight;
				if(USE_MOMENTUM) momentum[0][i][j] = tmpDeltaWeight;
			}
			
			// Do the hidden layer to output layer
			tmpDeltaWeight = (bDeltaOut * activationLevels[1][i]);
			if(USE_MOMENTUM) tmpDeltaWeight += MOMENTUM_FACTOR * momentum[1][i][0];
			
			weights[1][i][0] += tmpDeltaWeight;
			if(USE_MOMENTUM) momentum[1][i][0] = tmpDeltaWeight;
		}
	}
		
	private void clearActivationLevels() {
		for(int i=0; i<activationLevels.length; i++) {
			for(int j=0; j<activationLevels[i].length; j++) {
				activationLevels[i][j] = 0.0;
			}
		}
	}
	
	private double getNextInitialWeight() {
		if(useRandomInitialWeights) return ((rand.nextDouble()* INITIAL_WEIGHT_RANGE) - (INITIAL_WEIGHT_RANGE/2.0));
				//(1.0/numInputUnits) - (0.5/numInputUnits));
		else return initialWeight;
	}
	
	// Retrieve the outputs of the network
	public double[] getOutputs() {
		return activationLevels[activationLevels.length - 1];
	}
	
//	public double getRawOutput() {
//		return rawOutput;
//	}
	
//	public double getHighestWeight() {
//		double highestWeight = 0.0;
//		
//		for(int i=0; i<weights.length; i++) {
//			for(int j=0; j<weights[i].length; j++) {
//				for(int k=0; k<weights[i][j].length; k++) {
//					if(Math.abs(weights[i][j][k]) > Math.abs(highestWeight)) {
//						highestWeight = weights[i][j][k];
//					}
//				}
//			}
//		}
//		
//		return highestWeight;
//	}
	
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
	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("Hidden Weights:\n");
		for(int i=0; i< weights[1].length; i++) {
			for(int j=0; j< activationLevels[activationLevels.length - 1].length; j++) {
				sb.append(weights[1][i][j] + " | ");
			}
			sb.append(" || ");
		}
		
		sb.append("\nInput Weights:\n");
		for(int i=0; i<numInputUnits; i++) {
			for(int j=0; j<weights[0].length; j++) {
				sb.append( " | " + weights[0][i][j]);
			}
			sb.append(" || ");
		}
		sb.append("\n");
		
		return sb.toString();
	}
	
	
	// A test main
	public static void main(String[] args) {
		IOUtilitiesInterface iou = new ConsoleIOUtilities();
		
		int inputUnits = iou.getInt("How many input units? ");
		int hiddenUnits = iou.getInt("How many hidden units? ");
		int outputUnits = iou.getInt("How many output units? ");
		double[] inputs = new double[inputUnits];
		double targetOutput = 0;
		
		boolean convergence = iou.getYesNo("Do you want to go for convergence? ");
		
		RefacMLP myMLP = new RefacMLP(new int[] {inputUnits, hiddenUnits, outputUnits});
		myMLP.setLearningParams(0.1, .6);
		double output = 0;
		
		boolean notFinished = true;

		while(notFinished){
			for(int i=0; i<inputUnits; i++) {
				double input = iou.getDouble("What is input " + i + "? ");
				inputs[i] = input;
			}
			targetOutput = iou.getDouble("Target output: ");
			
			iou.println("\n");
			
			
			if(convergence) {
				double error = Double.MAX_VALUE;
				int iterations = 0;
				
				while(Math.abs(error) > convergenceThreshold) {
					try{
						output = myMLP.activate(inputs)[0];
						
						error = targetOutput - output;
						myMLP.backprop(error);
						iterations++;
					} catch(IncorrectInputsException e) {
						throw new RuntimeException(e); 
					}
					iou.println("Output: " + output);
				}
				
				iou.println("Converged after " + iterations + " iterations.\n");
				iou.println("Here is the network :\n" + myMLP.toString());
			}
			else {
				boolean cont = true;
				int iteration = 0;
				try{
				while(cont) {
					iou.println("Here is the network at iteration " + iteration + ":\n" +
							myMLP.toString());
					output = myMLP.activate(inputs)[0];
					iou.println("Output: " + output);
		
					cont = iou.getYesNo("Another iteration? ");
					iteration++;
					iou.print("\n");
					if(cont)myMLP.backprop(targetOutput - output);
				}
				}catch(IncorrectInputsException e) {
					iou.println("Caught an exception! Quitting ... " + e);}
				
				
			}
			
			notFinished = iou.getYesNo("Do you want to change the inputs and continue? ");
		}
	}
	public static boolean isSigmoidOutput() {
		return SIGMOID_OUTPUT;
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
