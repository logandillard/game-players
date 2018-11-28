package games;

import java.io.IOException;

public interface NNPlayer<G extends Game> extends GamePlayer<G> {
	
	double[] activateNN(G game, boolean NNisPlayer1) ;

	void loadModel() throws IOException, ClassNotFoundException ;

	void saveModel() throws IOException ;

	void initEligTraces();

	void updateElig();

	void TDLearn(double error);

	String getModelFile();

}
