package com.dillard.games.mancala;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.dillard.games.NNPlayer;
import com.dillard.games.NoLookNNPlayer;
import com.dillard.nn.IncorrectInputsException;
import com.dillard.nn.OldBrokenNeuralNetwork;

public class NNMancalaPlayer extends NoLookNNPlayer<Mancala> implements NNPlayer<Mancala> {
	public static final String MODEL_FILE = "model.ser";
	public static final double INITIAL_WEIGHT = 0.025;
	public static final boolean RANDOM_INITIAL_WEIGHT = true;
	public static final int NUM_HIDDEN_UNITS = 50;
	public static final double LAMBDA = 0.7;
	public static final double ALPHA = 0.0005;
	private OldBrokenNeuralNetwork neuralNet;
	private String serializedFile;
	private boolean unserialized;


	NNMancalaPlayer(String serializedFileName) {
		serializedFile = serializedFileName;

		try {
			loadModel();
			unserialized = true;
		} catch(Exception e) {
			unserialized = false;

			if(RANDOM_INITIAL_WEIGHT) {
				neuralNet = new OldBrokenNeuralNetwork(
						new int[] {numInputs, NUM_HIDDEN_UNITS, 1});
				// num input units, num hidden units, num output units
			}
			else {
				neuralNet = new OldBrokenNeuralNetwork(
						new int[] {numInputs, NUM_HIDDEN_UNITS, 1}, INITIAL_WEIGHT);
				// num input units, num hidden units, num output units
//				neuralNet = new RefacMLP(1, // num hidden layers
//						numInputs, // num input units
//						new int[] {NUM_HIDDEN_UNITS}, // num hidden units
//						1, INITIAL_WEIGHT);	// Num output units, initial weight
			}
		}

		neuralNet.setLearningParams(ALPHA, LAMBDA);
	}



	/* VERSION WITH LOOKAHEAD ON FREE MOVES
	 * static Double freeMoveValue;

	public Object move(Game game) {
		if(!(game instanceof Mancala)) throw new ClassCastException("Game is not Mancala. " +
				"This is a Mancala player only!");
		freeMoveValue = Double.MAX_VALUE;
		return moveInternal((Mancala)game);
	}

	private Object moveInternal(Mancala theGame) {
		// Setup
		boolean didFreeMove = false;
		Object[] moves = theGame.getMoves();
		boolean NNisPlayer1 = theGame.player1Turn();
		Object freeTurnMove;
		Mancala[] possibleStates = new Mancala[moves.length];
		Mancala tempGame;
		double tempVal = Double.MAX_VALUE;
		Object m;
		double lowestFound = Double.MAX_VALUE;
		int lowestFoundIndex = -1;

		// Generate all moves
		for(int i=0; i<moves.length; i++) {
			m = moves[i];

			// Perform each move
			tempGame = (Mancala)theGame.clone();
			try{
				tempGame.move(m);
			} catch(InvalidMoveException exc) {
				throw new IllegalArgumentException(exc);
				//continue;
			}
			possibleStates[i] = tempGame;

			// If it will still be the NNplayer's turn,
			// have it keep picking the best move and then report back
			// the game at that best state down the recursive tree
			if((NNisPlayer1 == tempGame.player1Turn()) && !tempGame.isTerminated()) {
				didFreeMove = true;
				freeTurnMove = this.move(tempGame);
				try{
					tempGame.move(freeTurnMove);
				} catch(InvalidMoveException exc) {
					throw new IllegalArgumentException(exc);
					//continue;
				}
			}
			else{
				didFreeMove = false;
			}

			// Calculate the neuralNet's predicted value for this move option.
			// Predicted value will be for the other player's chances, since it will
			// now be the other player's turn
			try{
				tempVal = (neuralNet.activate(boardToNNInputs(tempGame, NNisPlayer1)))[0];

				if(didFreeMove) {
					if(tempVal < freeMoveValue) freeMoveValue = tempVal;
					else tempVal = freeMoveValue;
				}

			} catch(IncorrectInputsException e) {
				throw new RuntimeException("Inputs to NN do not match expected inputs!");
			}

			// record the lowest value found so far (lowest chances of winning for the other player)
			if(tempVal < lowestFound) {
				lowestFound = tempVal;
				lowestFoundIndex = i;
			}
		}

		if(lowestFoundIndex < 0) {
			throw new ModelBlewUpException("Lowest found move index is " + lowestFoundIndex +
					" output is: " + neuralNet.getOutputs()[0]);
		}

		// return the move with the highest predicted value
		//System.out.println("Lowest found value: " + lowestFound);
		return moves[lowestFoundIndex];
	}
	 */




	static final boolean NEW_WAY = true;
	static final int FIRST_MULTI_INPUT = NEW_WAY ? 14 : 13;
	static final int numInputsForTurns = 2;
	static final int numInputsStonesEachSide = NEW_WAY ? 6 : 0;
	static final int numInputsScoreDiff = 8;
	static final int numInputsPerCloseToWinning = 5;
	static final int numInputsPerPit = NEW_WAY ? 20 : 19;
	static final int ZERO_FLAG_INDEX = NEW_WAY ? 17 : 16;
	static final int EXTRA_TURN_FLAG_INDEX = NEW_WAY ? 18 : 17;
	static final int CAPTURE_FLAG_INDEX = NEW_WAY ? 19 : 18;
	static final int numInputs = numInputsForTurns + (numInputsScoreDiff*2) +
					(numInputsPerCloseToWinning * 2) + numInputsStonesEachSide*2 +
					numInputsPerPit * (Mancala.NUM_PITS_PER_ROW * 2);

	@Override
    public double[] boardToNNInputs(Mancala game, boolean NNisPlayer1) {
		return NNMancalaPlayer.boardToNNInputsStatic(game, NNisPlayer1);
	}
	public static double[] boardToNNInputsStatic(Mancala game, boolean NNisPlayer1) {
		double[] NNInputs = new double[numInputs];

		// This player's stuff is always first: first turn indicator, first
		// score, first board row, etc.

		// Record the turn
		boolean isPlayer1Turn = game.isPlayer1Turn();
		NNInputs[0] = (NNisPlayer1 == isPlayer1Turn ? 1 : 0);
		NNInputs[1] = (NNisPlayer1 != isPlayer1Turn ? 1 : 0);

		int[] board = game.pitsToArray(NNisPlayer1);


		if(NEW_WAY) {
		// Record what stage of the game we're in by how many stones there are on each side
		// Record how many stones are left on the board
//		int remainingOnBoardIndex = numInputsForTurns +
//			numInputsScoreDiff + (numInputsPerCloseToWinning * 2);
//		int numStonesOnBoard = game.getNumStonesOnBoard();
//
//		int stonesRemainingRepresentation = (numStonesOnBoard - 1) / 8;
//		NNInputs[remainingOnBoardIndex + stonesRemainingRepresentation] = 1.0;


		// Record how many stones are left on each side
		int remainingOnBoardIndex = numInputsForTurns;
		int numStonesMySide = game.getNumStonesOnBoard(isPlayer1Turn);
		int numStonesOtherSide = game.getNumStonesOnBoard(!isPlayer1Turn);

		NNInputs[remainingOnBoardIndex + (numStonesMySide - 1) / 4] = 1.0;

		remainingOnBoardIndex += numInputsStonesEachSide;
		NNInputs[remainingOnBoardIndex + (numStonesOtherSide - 1) / 4] = 1.0;


//if(numStonesMySide + numStonesOtherSide != numStonesOnBoard)
//		throw new RuntimeException("Stones on board don't add up");
		}




		// Record the difference between the scores
		// Advantage is represented as follows: the first four inputs
		// each represent advantage of 2 points,
		// the next four each represent advantage of 4 points.
		int myScore = (int) game.getFinalScore(NNisPlayer1);
		int otherScore = (int) game.getFinalScore(!NNisPlayer1);
		int scoreDiff = myScore - otherScore;
		int scoreDiffIndex = numInputsForTurns + numInputsStonesEachSide*2;
		if(scoreDiff != 0) {
			if(scoreDiff < 0) {
				scoreDiffIndex += numInputsScoreDiff;
				scoreDiff *= -1;
			}

			for(int i=0; i<4; i++) {
				if(scoreDiff == 0) break;

				if(scoreDiff > 1) {
					NNInputs[scoreDiffIndex + i] = 1;
					scoreDiff -= 2;
				}
				else if(scoreDiff == 1) {
					NNInputs[scoreDiffIndex + i] = 0.5;
					scoreDiff--;
				}
			}

			scoreDiffIndex += 4;
			double tempScoreDiff;

			for(int i=0; i<4; i++) {
				if(scoreDiff == 0) break;

				if(scoreDiff > 4) {
					tempScoreDiff = 4.0;
				}
				else {
					tempScoreDiff = scoreDiff;
				}
				NNInputs[scoreDiffIndex + i] = tempScoreDiff / 4.0;
				scoreDiff -= tempScoreDiff;
			}
		}



		// Record how close each one is to winning (24 points = win)
		// Records the person's score - 10.
		// numInputsPerCloseToWinning = 5
		// the first two represent 4 points each, next three represent 2 points each
		// thus if I have 21 points, it will be 1.0, 1.0, 1.0, 0.50, 0.0
		int[] closeToWinning = new int[] {myScore - 10, otherScore - 10};
		double closeScore;
		int closeIndex = numInputsScoreDiff*2 + numInputsForTurns + numInputsStonesEachSide*2;
		for(int p=0; p<2; p++) {
			closeScore = closeToWinning[p];

			for(int i=0; i<2; i++) {
				if(closeScore > 3) {
					NNInputs[closeIndex + i] = 1.0;
					closeScore -= 4;
				}
				else if(closeScore > 0) {
					NNInputs[closeIndex + i] = closeScore / 4.0;
					closeScore = 0;
				}
				else break;
			}

			for(int i=2; i<5; i++) {
				if(closeScore > 1) {
					NNInputs[closeIndex + i] = 1.0;
					closeScore -= 2;
				}
				else if(closeScore == 1) {
					NNInputs[closeIndex + i] = 0.5;
					closeScore -=1;
				}
				else break;
			}

			closeIndex += numInputsPerCloseToWinning;
		}



		// Record the board state
		int stateIndexInit = numInputsForTurns + numInputsStonesEachSide*2 +
						numInputsScoreDiff*2 + (numInputsPerCloseToWinning * 2);
		int stateIndex;
		int seedsInPit;
		double tempSeeds;

		// For each pit (there are 12)
		int numPits = Mancala.NUM_PITS_PER_ROW * 2;
		for(int i=0; i<numPits; i++) {
			seedsInPit = board[i];
			stateIndex = stateIndexInit + (numInputsPerPit * i);

if(seedsInPit > 25) throw new RuntimeException("There were " + seedsInPit +
		" seeds in pit " + (i+1) + ". Current max allowed in model is 25!");

			if(seedsInPit > 0) {
				// Record the state of the pit

				if(false) {
				//==========================================================
				// One input represents which number (1-13) of seeds there are
				if(seedsInPit <= 13) {
					NNInputs[stateIndex + seedsInPit - 1] = 1.0;
					seedsInPit = 0;
				}
				else {
					NNInputs[stateIndex + 13] = 1.0;
					seedsInPit -= 14;
				}
				}
				//==========================================================
				else{
				// 13 inputs represent 1 seed each
				for(int j=0; j<13; j++) {
					if(seedsInPit == 0) break;

					NNInputs[stateIndex + j] = 1.0;
					seedsInPit--;
				}
				}


				// 3 inputs represent 4 seeds each
				for(int j=FIRST_MULTI_INPUT; j<ZERO_FLAG_INDEX; j++) {
					if(seedsInPit == 0) break;

					if(seedsInPit >= 4) {
						tempSeeds = 4.0;
					}
					else {
						tempSeeds = seedsInPit;
					}

					NNInputs[stateIndex + j] = tempSeeds / 4.0;
					seedsInPit -= tempSeeds;
				}
			}
			else { // Seeds in pit = 0
				// input 15 is a flag for pit has 0 seeds
				NNInputs[stateIndex + ZERO_FLAG_INDEX] = 1.0;
			}


			// Record other info about this pit

			// Record if it can get an extra turn
			if((board[i] % 13) + (i % 6) == 6) {
				NNInputs[stateIndex + EXTRA_TURN_FLAG_INDEX] = 1.0;
			}

			// Record if it can get a capture
			int endingIndex = (board[i] + (i%6)) % 13;
			if((endingIndex < 6) // If where it lands is on the same player's side
					&& board[endingIndex] == 0		// and where it lands is zero
					&& board[11 - endingIndex] != 0		// and the place opposite where it lands is not zero
					&& board[i] > 0 && board[i] <= 13) {

				NNInputs[stateIndex + CAPTURE_FLAG_INDEX] = 1.0;
			}
		} // End for each pit

		// check if something is wrong
//		for(double d : NNInputs) {
//			if(d > 1.0) {
//				throw new RuntimeException();
//			}
//		}

		return NNInputs;
	}




	public void initEligTraces() {
		neuralNet.reset();
	}
	public void updateElig() {
		neuralNet.updateElig();
	}
	public void TDLearn(double error) {
		neuralNet.tdLearn(new double[] {error});
	}
	public double[] activateNN(Mancala game, boolean isPlayer1) {
		try{
			double[] rawOut = neuralNet.activate(boardToNNInputs(game, isPlayer1));
			if(OldBrokenNeuralNetwork.isSigmoidOutput()) {
				for(int i=0; i<rawOut.length; i++) {
					rawOut[i] *= 2;
					rawOut[i] -= 1;
				}
			}
			return rawOut;
		}catch(IncorrectInputsException e) {
			throw new RuntimeException("Incorrect inputs: " + e);
		}
	}

	public void saveModel() throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serializedFile));
		oos.writeObject(neuralNet);
		oos.close();
	}
	public void loadModel() throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedFile));
		neuralNet = (OldBrokenNeuralNetwork)ois.readObject();
		ois.close();
	}

//	public double getHighestWeight() {
//		return neuralNet.getHighestWeight();
//	}

	public boolean unserialized() {
		return unserialized;
	}


//	@Override
//	protected String getModelBlewUpExceptionMessage() {
//		return "No details because refactoring made them hard to get and I don't care.";
//		return "Lowest found move index is " + highestFoundIndex +
//		" output is: " + neuralNet.getOutputs()[0];
//	}




	@Override
	protected double getNNScoreForGamestate(Mancala game, boolean isPlayer1) {
		return activateNN(game, isPlayer1)[0];
	}
	@Override
	public String getModelFile() {
		return MODEL_FILE;
	}


//	//Test NNInput representation for errors
//	public static void main(String[] args) {
//		Mancala mancala = new Mancala();
//		NNMancalaPlayer player = new NNMancalaPlayer(null);
//
//		double[] nnInputs = player.boardToNNInputs(mancala);
//
//		int i=100;
//	}
}
