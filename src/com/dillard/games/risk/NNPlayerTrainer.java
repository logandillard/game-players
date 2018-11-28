package com.dillard.games.risk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NNPlayerTrainer {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		System.out.println("Starting");
		
		// Create players
		List<RiskPlayer> opponents = new ArrayList<>();
//		opponents.add(new RiskPlayerConservative("Conservative"));
//		opponents.add(new RiskPlayerConservativeCardAware("ConservativeCard"));
//		opponents.add(new RiskPlayerAggressive("Aggressive"));
		opponents.add(new RiskPlayerRandom("Rando"));
//		opponents.add(new RiskPlayerRandomNoAttack("Lazy"));
		opponents.add(new RiskPlayerPartiallyLazy("Lazy 80%", 0.8));
//		opponents.add(new RiskPlayerPartiallyLazy("Lazy2 80%", 0.8));
//		opponents.add(new NNRiskPlayer("Non-learning NN"));
		
		NNRiskPlayer nnPlayer = new NNRiskPlayer("NNPlayer");
		nnPlayer.setLearningMode(true);
		
		if (new File(nnModelFile).exists()) {
		    nnPlayer.loadModel(nnModelFile);
		    System.out.println("Loaded neural network from file");
		} else {
		    System.out.println("Starting fresh with a new neural network");
		}

		NNPlayerTrainer trainer = new NNPlayerTrainer(opponents, nnPlayer);
		trainer.train(1000, 50);

		System.out.println("DONE");
	}

	@SuppressWarnings("unused")
	private List<RiskPlayer> opponents;
	private List<RiskPlayer> players;
	private NNRiskPlayer nnPlayer;
	private WinLossRecord winLossRecord;
	private static String nnModelFile = "/Users/Logan/risk/neural_network";
	
	public NNPlayerTrainer(List<RiskPlayer> opponents, NNRiskPlayer nnPlayer) {
		this.opponents = opponents;
		this.players = new ArrayList<>(opponents);
		this.players.add(nnPlayer);
		winLossRecord = newWinLossRecord();
		this.nnPlayer = nnPlayer;
	}

	private void train(final int numEpochs, final int numGamesPerEpoch) throws IOException {
		for (int i=0; i<numEpochs; i++) {
			trainOneEpoch(numGamesPerEpoch);
		     // save NN to disk
	        this.nnPlayer.saveModel(nnModelFile);
		}
	}

	private void trainOneEpoch(final int numGames) {
		winLossRecord = newWinLossRecord();
		long totalTime = 0;
		long gameSeed = System.currentTimeMillis(); // train with the same game seed for every game in the epoch
		for (int i=0; i<numGames; i++) {
			long start = System.currentTimeMillis();
			trainOneGame(gameSeed);
			totalTime += System.currentTimeMillis() - start;
		}
		System.out.println("Time per game ms: " + (totalTime/numGames));

		System.out.println("Winning ratios");
		for (String name : winLossRecord.getPlayerNames().stream().sorted().collect(Collectors.toList())) {
			System.out.println(name + "\t" + winLossRecord.getWinRatio(name));
		}
		System.out.println();
	}
	
	private WinLossRecord newWinLossRecord() {
		return new WinLossRecord(players.stream().map(RiskPlayer::getName).collect(Collectors.toList()));
	}

	private void trainOneGame(long gameSeed) {
		nnPlayer.resetLearning(); // reset eligibility traces

		// TODO add old NN players to opponents
		Collections.shuffle(players);
		RiskGame game = new RiskGame(players, gameSeed);
		game.setLogChanges(false);
		game.play();
		

		boolean isWinner = game.getWinner().equals(nnPlayer.getName());
		boolean isDraw = game.getWinner() == RiskGame.THE_CAT_PLAYER_NAME;
		nnPlayer.learnFinalResult(isWinner, isDraw, game.getState());

		winLossRecord.addWin(game.getWinner());

//		System.out.println("Ended on turn number " + game.getState().getTurnNumber());
//		System.out.println(game.getState().getTurnNumber());
	}
}
