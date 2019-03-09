package com.dillard.games;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.dillard.nn.OldBrokenNeuralNetwork;


public abstract class ABNNPlayer<M extends Move, G extends Game<M, G>> extends ABPruningPlayer<M, G> {
	protected OldBrokenNeuralNetwork neuralNet;
	private String modelFile;

	public ABNNPlayer (int turnDepthLim, String modelFile, boolean forTraining) throws Exception {
		super(turnDepthLim);
		this.modelFile = modelFile;

		if (forTraining) {
			createModel();
		} else {
			loadModel();
		}
	}

	protected abstract void createModel() ;

	@Override
	protected abstract double evaluate(G theGame) ;

	public void loadModel() throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile));
		neuralNet = (OldBrokenNeuralNetwork)ois.readObject();
		ois.close();
	}
}
