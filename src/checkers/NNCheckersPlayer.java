package checkers;

import java.io.IOException;

import games.ABNNPlayer;
import games.IncorrectInputsException;
import games.RefacMLP;

public class NNCheckersPlayer extends ABNNPlayer<Checkers> {
	private static final int INPUT_LAYER_SIZE = 50;
	private static final int FIRST_HIDDEN_LAYER_SIZE = 50;
	private static final int SECOND_HIDDEN_LAYER_SIZE = 5;
	private static final int OUTPUT_LAYER_SIZE = 1;
	
	public static final double LAMBDA = 0.7;
	public static final double ALPHA = 0.0005;
	
	public NNCheckersPlayer(int turnDepthLimit, String modelFile, boolean forTraining) throws Exception {
		super(turnDepthLimit, modelFile, forTraining);
	}

	
	@Override
	protected double evaluate(Checkers game) throws Exception {
		return getNNScoreForGamestate(game, super.isPlayer1());
	}

	protected double getNNScoreForGamestate(Checkers game, boolean isPlayer1) throws IncorrectInputsException {
		double[] nnOutputs = neuralNet.activate(boardToNNInputs(game, isPlayer1));
		return nnOutputs[0];
	}

	protected double[] boardToNNInputs(Checkers game, boolean NNisPlayer1) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected void createModel() {
		neuralNet = new RefacMLP(new int[] {
				INPUT_LAYER_SIZE, 
				FIRST_HIDDEN_LAYER_SIZE, 
				SECOND_HIDDEN_LAYER_SIZE, 
				OUTPUT_LAYER_SIZE});
		neuralNet.setLearningParams(ALPHA, LAMBDA);
	}


	@Override
	public double[] activateNN(Checkers game, boolean NNisPlayer1) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void saveModel() throws IOException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void initEligTraces() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void updateElig() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void TDLearn(double error) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String getModelFile() {
		// TODO Auto-generated method stub
		return null;
	}

}
