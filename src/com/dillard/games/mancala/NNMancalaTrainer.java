package com.dillard.games.mancala;

import java.io.IOException;

import com.dillard.games.ABPruningPlayer;
import com.dillard.games.ConsoleIOUtilities;
import com.dillard.games.GamePlayer;
import com.dillard.games.IOUtilitiesInterface;
import com.dillard.games.NNPlayerTrainer;
import com.dillard.games.RandomPlayer;
import com.dillard.nn.ModelBlewUpException;

public class NNMancalaTrainer  {

	static final boolean BENCHMARK_INITIALLY = false;
	static final double RANDOM_MOVE_PCT = .02;
	static final int BENCHMARK_GAMES = 500;
	static final int NUM_TO_LOG = 10000;
	static final int NUM_TO_REPORT = 1000;
	static final int NUM_TO_LOAD_OPP = 1000;
	static final int SAVE_MODEL_GAMES = 25000;
	static final String LOG_FILE = "benchmarkLog.tsv";
	static final boolean TRAIN_ONLINE = true;
	static final boolean MANUAL_TRAIN = false;

	static final boolean PUBLISH = false;
	static final int PUBLISH_GAMES = 9000;
	static final int PUBLISH_INTERVAL = 1000;
	static final int PUBLISH_BENCH_GAMES = 1000;


	public static void main(String[] args) throws Exception {
		NNPlayerTrainer<MancalaMove, Mancala> trainer = new NNPlayerTrainer<>();
		boolean cont = true;
		IOUtilitiesInterface iou = new ConsoleIOUtilities();
		int numGames;
		String trainType;
		char trainTypeChar;
		Mancala prototypeGame = new Mancala();

		// Create players
		NNMancalaPlayer nnPlayer = new NNMancalaPlayer(NNMancalaPlayer.MODEL_FILE);
		//NNMancalaPlayer nnPlayer2= new NNMancalaPlayer(NNMancalaPlayer.MODEL_FILE);;
		GamePlayer<MancalaMove, Mancala> randPlayer = new RandomPlayer<>();
		GamePlayer<MancalaMove, Mancala> abPruning1 = new ABPruningPlayer<>(1);
		GamePlayer<MancalaMove, Mancala> abPruning2 = new ABPruningPlayer<>(2);
		GamePlayer<MancalaMove, Mancala> abPruning3 = new ABPruningPlayer<>(3);
		GamePlayer<MancalaMove, Mancala> abPruning4 = new ABPruningPlayer<>(4);

		// Set benchmarking players (opponents)
		GamePlayer<MancalaMove, Mancala>[] benchOpponents = new GamePlayer[] {abPruning2, abPruning1}; //, abPruning1, randPlayer
		String[] benchOpponentsNames = new String[] {"AB-2", "AB-1"}; //"AB-4", "AB-3",, "AB-1", "Rand"

		// Set the training opponents
		GamePlayer<MancalaMove, Mancala>[] trainOpponents = new GamePlayer[] {nnPlayer};

		iou.println(nnPlayer.unserialized() ? "Unserialized model correctly!\n" :
			"Was NOT able to unserialize model.\n");

		// Do manual train
//		if(MANUAL_TRAIN) {
//			System.out.println("Doing a manual training game...\n");
//			trainer.manualTrain(nnPlayer, iou);
//			return;
//		}

		if(PUBLISH) {
			System.out.println("Publishing...\n");
			trainer.publishModel(PUBLISH_GAMES, PUBLISH_INTERVAL, PUBLISH_BENCH_GAMES,
					nnPlayer, trainOpponents, benchOpponents, benchOpponentsNames, prototypeGame);
			return;
		}

		long startTime;
		long endTime;
		double[][] benchmarkResults = null;

		// Benchmark
		if(BENCHMARK_INITIALLY) benchmarkResults = trainer.benchmark(new Mancala(), nnPlayer, BENCHMARK_GAMES, benchOpponents);

		while(cont) {
			if(benchmarkResults != null)
			iou.println(trainer.benchmarkResultsDisplay(BENCHMARK_GAMES, benchmarkResults, benchOpponentsNames));

			//trainType = iou.getString("\nTrain full games or partial games from random beginnings? (f or r) ");
			trainType = "f";
			numGames = iou.getInt("How many games to train for? (0 to quit) ");

			trainTypeChar = trainType.toLowerCase().charAt(0);

			if(numGames > 0) {
				startTime = System.currentTimeMillis();

				switch(trainTypeChar) {
				// Train

				case 'f':
					trainer.managedTrain(nnPlayer, numGames, trainOpponents, benchOpponents, benchOpponentsNames,
						NUM_TO_REPORT, NUM_TO_LOG, SAVE_MODEL_GAMES, BENCHMARK_GAMES, prototypeGame );
						break;
				case 'r':
					trainer.managedTrainFromRandomPositions(nnPlayer, prototypeGame, numGames, new GamePlayer[] {nnPlayer});
					break;
				}
				endTime = System.currentTimeMillis();

				long duration = endTime - startTime;
				iou.println("Training took " + (duration/3600000) + ":" + ((duration%3600000)/60000) +
						":" + ((duration%60000)/1000) + "." + (duration%1000));



				// Benchmark
				iou.print("Benchmarking...");
				startTime = System.currentTimeMillis();
				try{
					benchmarkResults = trainer.benchmark(new Mancala(), nnPlayer, BENCHMARK_GAMES, benchOpponents);
				}catch(ModelBlewUpException exc) {
					iou.println("Blew up while benchmarking: " + exc.toString());
				}
				endTime = System.currentTimeMillis();
				duration = endTime - startTime;
				iou.println("   (took " + (duration/60000) + ":" + ((duration%60000)/1000) + ":" +
						(duration%1000) +" ms)\n");
			}
			else cont = false;
		}

		try{
			nnPlayer.saveModel();
			iou.println("\nSaved model.");
		}catch(IOException exc) {
			throw new RuntimeException("Could not save!!");
		}
		iou.println("\nGoodbye.");

	} //End main
}
