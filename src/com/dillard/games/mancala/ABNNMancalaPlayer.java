package com.dillard.games.mancala;

import java.io.IOException;

import com.dillard.games.ABNNPlayer;

public class ABNNMancalaPlayer extends ABNNPlayer<Mancala> {

	public ABNNMancalaPlayer (int turnDepthLim, String modelFile) throws Exception {
		super(turnDepthLim, modelFile, false);
	}

	@Override
	protected double evaluate(Mancala theGame) {
	try{
		return neuralNet.activate(NNMancalaPlayer.boardToNNInputsStatic(theGame, super.isPlayer1()))[0];
	} catch(Exception e) {
		throw new RuntimeException(e);
	}
}

	@Override
	protected void createModel() {
		// TODO Auto-generated method stub

	}

	@Override
	public double[] activateNN(Mancala game, boolean NNisPlayer1) {
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
