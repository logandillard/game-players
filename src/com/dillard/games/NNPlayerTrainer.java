package com.dillard.games;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import com.dillard.games.GamePlayer;
import com.dillard.games.InvalidMoveException;
import com.dillard.games.RandomPlayer;

public class NNPlayerTrainer<G extends Game>  {

	protected static final boolean BENCHMARK_INITIALLY = false;
	public static final double RANDOM_MOVE_PCT = .02;
	public static final int BENCHMARK_GAMES = 500;
	public static final int NUM_TO_LOG = 10000;
	public static final int NUM_TO_REPORT = 1000;
	public static final int NUM_TO_LOAD_OPP = 1000;
	public static final int SAVE_MODEL_GAMES = 25000;
	public static final String LOG_FILE = "benchmarkLog.tsv";
	public static final boolean TRAIN_ONLINE = true;
	public static final boolean MANUAL_TRAIN = false;
	
	public static final boolean PUBLISH = false;
	public static final int PUBLISH_GAMES = 9000;
	public static final int PUBLISH_INTERVAL = 1000;
	public static final int PUBLISH_BENCH_GAMES = 1000;
	
	
	
	// TRAINING LOGIC
	public void managedTrain(NNPlayer<G> nnPlayer, int numGamesToPlay, GamePlayer<G>[] opponents, 
			GamePlayer<G>[] benchmarkOpponents, String[] benchmarkOppNames, 
			int numToReport, int numToLog, int numToSaveModel, int benchmarkGames,
			G prototypeGame) throws Exception {

		double[][] benchmarkResults;
		String logString;
		
		// TODO this assumes that minTrain is a divisor of all these other numbers. this should be enforced
		int minTrain = Math.min(numGamesToPlay, numToReport);
		minTrain = Math.min(minTrain, numToLog);
		minTrain = Math.min(minTrain, NUM_TO_LOAD_OPP);
		
		
		
		// Load the model for any NN opponents that are not nnPlayer (assume the model has been saved)
		try{
			for(int opponentIndex =0; opponentIndex < opponents.length; opponentIndex++) {
				if(opponents[opponentIndex] instanceof NNPlayer && 
						opponents[opponentIndex] != nnPlayer) {
					((NNPlayer<G>)opponents[opponentIndex]).loadModel();
				}
			}
		}catch (java.io.FileNotFoundException e) {
			//System.out.println("Could not load model in NN opponent - the model file was not found.");
//			for(int opponentIndex =0; opponentIndex < opponents.length; opponentIndex++) {
//				if(opponents[opponentIndex] instanceof NNPlayer && 
//						opponents[opponentIndex] != nnPlayer) {
//					opponents[opponentIndex] = new NNPlayer(NNPlayer.MODEL_FILE);
//				}
//			}
		}catch (Exception e) {
			System.out.println("Could not load model in NN opponent: " + e);
		}
		
		if(numGamesToPlay >= numToLog) {
			// Write the header to the log file
			logString = makeResultsHeader(benchmarkOppNames);
			writeToLog(logString);
		}
		
		
		
		// For the number of games to play
		int i;
		for(i=0; i < numGamesToPlay; i++) {
			
			// Train for minTrain games
			try{
				train(nnPlayer, minTrain, opponents, prototypeGame);
				i += minTrain - 1;
			}catch(Exception exc) {
				System.out.println("Caught an exception: " + exc.toString() + "\nQuiting training...");
				break;
			}
			
			
			// Load the model for any NN opponents that are not nnPlayer
			if(i % NUM_TO_LOAD_OPP == (NUM_TO_LOAD_OPP - 1) ) {
				try{
					boolean modelSaved = false;
					for(int opponentIndex =0; opponentIndex < opponents.length; opponentIndex++) {
						if(opponents[opponentIndex] instanceof NNPlayer && 
								opponents[opponentIndex] != nnPlayer) {
							if(!modelSaved) {
								nnPlayer.saveModel();
								modelSaved = true;
							}
							((NNPlayer<G>)opponents[opponentIndex]).loadModel();
						}
					}
				}catch (Exception e) {
					throw new RuntimeException("Could not load model in NN opponent: " + e);
				}
			} 
			
			// Save the model
			if(i % numToSaveModel == (numToSaveModel - 1)) {
				try{
					nnPlayer.saveModel();
					System.out.println("Saved model.");
				}catch(IOException exc) {
					System.out.println("Couldn't save model: " + exc);
				}
			}
			
			// Print status report
			if(i % numToReport == (numToReport - 1) && (i+1 != numGamesToPlay)) {
				System.out.println("Finished " + (i+1) + " games so far...");
			}
			
			// Benchmark the player
			if(i % numToLog == (numToLog - 1)) {
				benchmarkResults = benchmark((G) prototypeGame.clone(), nnPlayer, benchmarkGames, benchmarkOpponents);
				
				// Log the results
				logString = makeResultsString(i+1, benchmarkResults);
				writeToLog(logString);
				System.out.println("Wrote benchmark results to log file.");
			}

		}
		
		
		
		
		if(numGamesToPlay >= numToLog) writeToLog("\n\n");
		
		
		// Print how many games we actually trained for
		System.out.println("Actually trained for " + i + " games.");
		
		System.out.println("Did NOT save model - trying not to waste time.");
//		try{
////			nnPlayer.saveModel();
////			System.out.println("Saved model.");
//		}catch(IOException exc) {
//			throw new RuntimeException("Could not save!!");
//		}

	}
	
	
	
	private void train(NNPlayer<G> nnPlayer, int numGamesToPlay, GamePlayer<G>[] opponents, G prototypeGame) {

		double avgAbsError = 0.0;
		boolean tmpPlayer1Turn;
		G game;
		GamePlayer<G> opponent;
		int numGamesPlayed;
		int goFirstValue = (int) Math.round(Math.random());
		
		// For the number of games to play
		for(numGamesPlayed = 0; numGamesPlayed<numGamesToPlay; numGamesPlayed++) {
			opponent = opponents[numGamesPlayed % opponents.length];
			
			// Initialize eligibility traces and game
			nnPlayer.initEligTraces();
			game = (G) prototypeGame.clone();

			
			// Alternate this player being player 1 or player 2
			if(numGamesPlayed % 2 == goFirstValue) {
				tmpPlayer1Turn = game.isPlayer1Turn();
				while(game.isPlayer1Turn() == tmpPlayer1Turn) {	// While still this player's turn
					try{
						game.move(opponent.move(game));		// move
					}catch(Exception exc) {
						throw new RuntimeException(exc);

					}
				}
			}
						
			avgAbsError += trainFinishGame(nnPlayer, game, opponent, RANDOM_MOVE_PCT);
		}
		
		avgAbsError /= ((double)numGamesPlayed);
		System.out.print("   Avg abs error: " + avgAbsError + "\n");
	}
	
	
	
	// Returns the average error in the game
	private double trainFinishGame(NNPlayer<G> nnPlayer, G game, GamePlayer<G> opponent, 
			double randomMovePct) {
		double gameAvgAbsError = 0.0;
		int gameNumErrorCalc = 0;
		double reward = 0.0;
		double error = 0.0;
		double output = 0.0;
		Move m;
		
		// Done above in train()
		//nnPlayer.initEligTraces();
		
		boolean NNisPlayer1 = game.isPlayer1Turn();
		
		double oldOutput = (nnPlayer.activateNN(game, NNisPlayer1))[0];
		nnPlayer.updateElig();
		
		// MAIN LOOP OF A TRAINING GAME
		do {

			try{
			
				// Get the NNplayer's move and make it
				if(NNisPlayer1 == game.isPlayer1Turn() && !game.isTerminated()) {
					if(Math.random() < randomMovePct) {
						// Make a random move if appropriate based on the random move percent param
						List<Move> moves = game.getMoves();
						game.move(moves.get((int)(Math.random() * moves.size())));
						
						// Update the oldOutput, but don't set eligibilities or learn, 
						// as the interpretation of this state isn't eligible for 
						// credit or blame because we didn't actually choose it.
						oldOutput = (nnPlayer.activateNN(game, NNisPlayer1))[0];
						
						// NO - this doesn't work well
						// Reset the eligibilities because what came before has been disrupted,
						// and therefore no longer deserves credit or blame for what happens now
						//nnPlayer.initEligTraces();
						
						continue;
					}
					else {
					
						m = nnPlayer.move(game);
						game.move(m);
						
						//==================================
						// Get the output from the NN so updating eligibilities works right
						output = (nnPlayer.activateNN(game, NNisPlayer1))[0];
						nnPlayer.updateElig();
						//==================================
					}
				}
				else {
				
//				//==================================
//				// Get the output from the NN so updating eligibilities works right
//				oldOutput = (nnPlayer.activateNN(game, NNisPlayer1))[0];
//				nnPlayer.updateElig();
//				//==================================
				
					// Opponent moves now
					while((game.isPlayer1Turn() != NNisPlayer1) && !game.isTerminated()) {
							game.move(opponent.move(game));
					}
					
					// Get the updated output from the NN
					output = (nnPlayer.activateNN(game, NNisPlayer1))[0];
				}
			}catch(Exception exc) {
				throw new RuntimeException(exc);
			}
			
			//==================================
			if(TRAIN_ONLINE) {
				// Calculate reward and error
				// Reward = 1 for win, -1 for loss, 0 for incomplete game
				if(game.isTerminated()) {
					break;
				}
				else {
					// reward = 0;
					error = output - oldOutput; // + reward;
					gameAvgAbsError += Math.abs(error);
					gameNumErrorCalc++;
				}
				
				// Learn
				nnPlayer.TDLearn(error);
			}

			
			// Get the updated output from the NN
			oldOutput = (nnPlayer.activateNN(game, NNisPlayer1))[0];
			//oldOutput = output;
			
			// Update eligibility traces
			//nnPlayer.updateElig();
			
			
		} while(!game.isTerminated());
		
		
		// Learn again now that the game is over (the most important learning,
		// since this is the only time there could be a reward)
			
		// Record final updated output and update elig traces
		// NOT SURE IF IT SHOULD DO THIS!!
//			try{
//				// Get the updated output from the NN
//				output = (neuralNet.activate(boardToNNInputs(game)))[0];
//			}catch(Exception exc) {
//				throw new RuntimeException(exc);
//			}
//			neuralNet.updateElig();
		
		
		// Calculate reward and error, and learn
		double scoreDiff = NNisPlayer1 ? 
				(game.getFinalScore(true) - game.getFinalScore(false)) :
					(game.getFinalScore(false) - game.getFinalScore(true));
					
		if(scoreDiff > 0) reward = 1;
		else if(scoreDiff < 0 ) reward = -1;
		else reward = 0;
		
		
		//error = reward + (nnPlayer.gamma()*output) - oldOutput;
		error = reward - oldOutput;
		
		nnPlayer.TDLearn(error);
		
		gameAvgAbsError += Math.abs(error);
		gameNumErrorCalc++;
		return gameAvgAbsError / ((double) gameNumErrorCalc);
	}

	
	
	
	
	
	
	public void managedTrainFromRandomPositions(NNPlayer<G> nnPlayer, G prototypeGame, int numGamesToPlay,
			GamePlayer<G>[] opponents//, GamePlayer[] benchmarkOpponents, String[] benchmarkOppNames 
			) //int numToReport, int numToLog, int numToSaveModel, int benchmarkGames )
	{
for(int l=0; l<5; l++) {
		int numRandPly = 20;
		int randPlyDecreaseAmount = 1;
		int randPlyDecreaseInterval = 
			(int)Math.ceil((numGamesToPlay*randPlyDecreaseAmount) /(double)numRandPly);
		double avgAbsError = 0.0;
		double totalAvgAbsError = 0.0;
		

		int i;

	
		for(i = 0; i<numGamesToPlay; i++) {
			if(i % randPlyDecreaseInterval == randPlyDecreaseInterval - 1) numRandPly -= randPlyDecreaseAmount;
			
			try{
				avgAbsError += trainFromRandomPosition(nnPlayer, opponents[i%opponents.length], prototypeGame, numRandPly);
			
				if(i% NUM_TO_REPORT == NUM_TO_REPORT - 1) {
					System.out.println("Finished " + (i+1) + " games so far...\n" +
									"   Avg abs error: " + (avgAbsError/(double) NUM_TO_REPORT));
					totalAvgAbsError += avgAbsError;
					avgAbsError = 0.0;
				}
			}catch(Exception e) {
				System.out.println("Game already terminated. Number of random ply: " + numRandPly);
				i--;
			}
		}
		totalAvgAbsError += avgAbsError; 
		totalAvgAbsError /= ((double)i);
		System.out.print("   Total Avg abs error: " + totalAvgAbsError + "\n");
}
	}
	
	
	private double trainFromRandomPosition(NNPlayer<G> nnPlayer, GamePlayer<G> opponent, 
			G prototypeGame, int numRandomPly){
		G game = (G) prototypeGame.clone();
		RandomPlayer randPlayer = new RandomPlayer();
		
		try{
			for(int i=0; i<numRandomPly && !game.isTerminated(); i++) {
				game.move(randPlayer.move(game));
			}
		}catch(InvalidMoveException exc) {
			throw new RuntimeException("Problem with randPlayer!  " + exc.toString());
		}
		
		if(game.isTerminated()) { 
			throw new RuntimeException("Game is already terminated!");
		}
		else	
			return trainFinishGame(nnPlayer, game, opponent, 0.0);
	}
	
	
			
	
	
	
	public static String PUBILSH_LOG_FILE = "publishLog.txt";
	public void publishModel(int trainTotal, int trainInterval, int benchmarkGames, 
			NNPlayer<G> nnPlayer, GamePlayer<G>[] opponents, 
			GamePlayer<G>[] benchOpponents, String[] benchOpponentsNames,
			G prototypeGame) throws Exception {
		double[][] benchmarkResults;
		String logString;
		

		
		// Rename NNPlayer.MODEL_FILE to originalModel
		File initial = new File(nnPlayer.getModelFile());
		File temp = new File("original_model.ser");
		initial.renameTo(temp);

		// Write log file header
		logString = makeResultsHeader(benchOpponentsNames);
		writeToLog(logString);
		
		// Play the games and log the results
		for(int i=0; i<trainTotal; ) {
			// train trainInterval games
			train(nnPlayer, trainInterval, opponents, prototypeGame);
			
			i += trainInterval;
			
			// save model to publishName + gamesTrained
			try{
				nnPlayer.saveModel();
				File savedModel = new File(nnPlayer.getModelFile());
				File storedModel = new File(i + nnPlayer.getModelFile());
				savedModel.renameTo(storedModel);
			}catch(Exception e) {
				throw new RuntimeException("Couldn't save model!");
			}
			
			// Benchmark the player
			benchmarkResults = benchmark((G) prototypeGame.clone(), nnPlayer, benchmarkGames, benchOpponents);
			
			// Log the results
			logString = makeResultsString(i, benchmarkResults);
			writeToLog(logString);
			System.out.print(logString);
			System.out.println("Wrote benchmark results to log file.\n");
		}
		
		
		// rename original model to model.ser
		File current = new File(nnPlayer.getModelFile());
		current.delete();
		temp.renameTo(initial);
		
		// Rename the publish log file
		File log = new File(LOG_FILE);
		File publishLog = new File(PUBILSH_LOG_FILE);
		log.renameTo(publishLog);
	}
	
	
//	private static void manualTrain(NNPlayer NNplayer, IOUtilitiesInterface iou) {
//		train(NNplayer, 1, new GamePlayer[] {new HumanMancalaPlayer(1, 12, 1, iou)});
//	}
	
	
// BENCHMARK
	
	public static <G extends Game> double[][] benchmark(G gamePrototype, NNPlayer<G> nnPlayer, int numGames, GamePlayer<G>[] opponents) 
	throws Exception {
		double[] avgScoreDiffs = new double[opponents.length];
		double[] winRatio = new double[opponents.length];
		double[] lowScores = new double[opponents.length];
		double[] highScores = new double[opponents.length];
		G game;
		Move move;
		int lowScore =Integer.MAX_VALUE, highScore=-Integer.MAX_VALUE;
		int scoreDiff;
		boolean NNIsPlayer1;

		
		for(int i=0; i<opponents.length; i++) {
			lowScore = Integer.MAX_VALUE;
			highScore = -Integer.MAX_VALUE;
			
			for(int j=0; j<numGames; j++) {
				game = (G) gamePrototype.clone();
				
				// Alternate who moves first
				NNIsPlayer1 = (j%2 == 0 ? true : false);
				
				// Play the game
				while(!game.isTerminated()) {
					// If it is the NN player's turn
					if(game.isPlayer1Turn() == NNIsPlayer1) {
						// Get the next move from the NNplayer
						move = nnPlayer.move(game);
					}
					else { // Other player's turn
						move = opponents[i].move(game);
					}

					
					// Make the move
					try {
						game.move(move);
					} catch(InvalidMoveException exc) {
						System.out.println("That was not a valid move... try again.\n");
					}
				}
				
				
				// Record the outcome
				scoreDiff = (int) (game.getFinalScore(NNIsPlayer1) - game.getFinalScore(!NNIsPlayer1));
				if(scoreDiff < lowScore) {
					lowScore = scoreDiff;
				}
				if(scoreDiff > highScore) {
					highScore = scoreDiff;
				}
				avgScoreDiffs[i] += scoreDiff;
				if(scoreDiff != 0) {
					winRatio[i] += (scoreDiff > 0 ? 1 : -1);
				}
			}
			
			winRatio[i] = winRatio[i] / (double) numGames;
			avgScoreDiffs[i] = avgScoreDiffs[i] / (double) numGames;
			lowScores[i] = lowScore;
			highScores[i] = highScore;

		}
		
		return new double[][] {winRatio, avgScoreDiffs, highScores, lowScores};
	}
	
	
	
	
	// PRINTING AND FORMATTING METHODS
	
	private static String makeResultsHeader(String[] names) {
		StringBuffer tsvOut = new StringBuffer();
		
		tsvOut.append("Games\t");
		
		// The names for the scores
		for(String name: names) {
			tsvOut.append(name + "\t");
		}
		
		// The names + av for averages
		for(String name: names) {
			tsvOut.append(name + " av\t");
		}
		
		// The name + hi, name + lo
		for(String name: names) {
			tsvOut.append(name + " hi\t" + name + " lo\t");
		}
		
		tsvOut.append("\n");
		return tsvOut.toString();
	}


	protected static String makeResultsString(int gamesPlayed, double[][] benchmarkResults) {
		StringBuffer tsvOut = new StringBuffer();
		
		// The games played
		tsvOut.append(gamesPlayed + "\t");
		
		// The benchmark score against each player
		for(double bscore : benchmarkResults[0]) {
			tsvOut.append(bscore + "\t");
		}
		
		// The average game score differential
		for(double avg : benchmarkResults[1]) {
			tsvOut.append(avg + "\t");
		}
		
		// The game score differential high and low
		for(int i=0; i<benchmarkResults[2].length; i++) {
			tsvOut.append(benchmarkResults[2][i] + "\t" + benchmarkResults[3][i] + "\t");
		}
		
		tsvOut.append("\n");
		return tsvOut.toString();
	}


	public static String benchmarkResultsDisplay(int benchmarkGames, double[][] benchmarkResults, 
			String[] benchOpponentsNames) {
		DecimalFormat scoreFormat = new DecimalFormat("0.00");
		DecimalFormat pointFormat = new DecimalFormat("00.00");
		StringBuffer sb = new StringBuffer();
		sb.append("Benchmark results (over " + BENCHMARK_GAMES + " games):\n" +
		"(1 = all wins, -1 = all loses, 0 = equal wins/losses or all ties)\n");

		sb.append("\tBenchmark score\tAvg point diff  High   Low\n");
		for(int i = 0; i<benchmarkResults[0].length; i++) {
			sb.append(benchOpponentsNames[i] + "\t" + 
				scoreFormat.format(benchmarkResults[0][i]) + "\t\t" +
				pointFormat.format(benchmarkResults[1][i]) + "\t\t" +
				benchmarkResults[2][i] + "  " +
				benchmarkResults[3][i] + "\n");
		}
		return sb.toString();
	}
	
	
	protected static void writeToLog(String logString) {
		FileWriter fwrite;
		
		try{
			fwrite = new FileWriter(LOG_FILE, true);//Open log file for append
			fwrite.write(logString);
			fwrite.flush();
			fwrite.close();
		}catch(IOException exc) {
			throw new RuntimeException(exc);
		}
	}
	
	


}
