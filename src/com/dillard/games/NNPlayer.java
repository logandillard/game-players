package com.dillard.games;

import java.io.IOException;

public interface NNPlayer<M extends Move, G extends Game<M, G>> extends GamePlayer<M, G> {

	double[] activateNN(G game, boolean NNisPlayer1) ;

	void loadModel() throws IOException, ClassNotFoundException ;

	void saveModel() throws IOException ;

	void initEligTraces();

	void updateElig();

	void TDLearn(double error);

	String getModelFile();

}
