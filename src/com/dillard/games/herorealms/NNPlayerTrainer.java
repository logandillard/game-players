package com.dillard.games.herorealms;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NNPlayerTrainer {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting");

		// Create/load NN player
		NNHRPlayer nnPlayer = new NNHRPlayer("NNPlayer", 456);
		if (new File(nnModelFileLatest).exists()) {
		    nnPlayer.loadModel(nnModelFileLatest);
		    System.out.println("Loaded neural network from file");
		} else {
		    System.out.println("Starting fresh with a new neural network");
		}

		NNPlayerTrainer trainer = new NNPlayerTrainer(nnPlayer);
		trainer.train(100, 1000);

		System.out.println("DONE");
	}

	private List<HRPlayer> players;
	private NNHRPlayer nnPlayer;
	private WinLossRecord winLossRecord;
	private double maxScore = 0;
	public static String nnModelDir = "/Users/logan/game-players/";
	public static String nnModelFileBest = nnModelDir + "hero-realms-neural-network";
	public static String nnModelFileLatest = nnModelDir + "hero-realms-neural-network-latest";

	public NNPlayerTrainer(NNHRPlayer nnPlayer) {
		this.nnPlayer = nnPlayer;
	}

	private void train(final int numEpochs, final int numGamesPerEpoch) throws Exception {
	    // Initially measure our current best score from the existing best model file
        if (new File(nnModelFileBest).exists()) {
            NNHRPlayer bestModelPlayer = new NNHRPlayer("NNPlayer", 456);
            nnPlayer.loadModel(nnModelFileBest);
            maxScore = benchmark(bestModelPlayer, new HRPlayerSimple(), 5000);
        }

		for (int i=0; i<numEpochs; i++) {
			trainOneEpoch(numGamesPerEpoch);

		    double winRatio = benchmark(nnPlayer, new HRPlayerSimple(), 5000);

		    if (winRatio > maxScore) {
		        maxScore = winRatio;
    		     // save NN to disk
		        System.out.println("**** Saving new best model ****\n");
    	        this.nnPlayer.saveModel(nnModelFileBest);
		    }

		    this.nnPlayer.saveModel(nnModelFileLatest);
		}
	}

	private void trainOneEpoch(final int numGames) {
		winLossRecord = new WinLossRecord();
		nnPlayer.setLearningMode(true);
		nnPlayer.setMakeRandomMoves(true);
		long totalTime = 0;
		long gameSeed = System.currentTimeMillis();
		for (int i=0; i<numGames; i++) {
			long start = System.currentTimeMillis();
			trainOneGame(gameSeed);
			totalTime += System.currentTimeMillis() - start;
		}
		System.out.println("Time per game ms: " + (totalTime/numGames));

//		System.out.println("Winning ratios");
//		for (String name : winLossRecord.getPlayerNames().stream().sorted().collect(Collectors.toList())) {
//			System.out.println(name + "\t" + winLossRecord.getWinRatio(name));
//		}
		System.out.println();
	}

    private double benchmark(NNHRPlayer player, HRPlayer opponent, final int numGames) {
        WinLossRecord winLossRecord = new WinLossRecord();
        List<HRPlayer> players = Arrays.asList(opponent, player);
        player.setLearningMode(false);
        player.setMakeRandomMoves(false);
        for (int i=0; i<numGames; i++) {
            Collections.shuffle(players);
            HRGame game = new HRGame(players, System.currentTimeMillis());
            game.setLogChanges(false);
            game.play();

            String winnerName = game.getWinner();
            if (winnerName == null) {
                winnerName = "DRAW";
            }
            winLossRecord.addWin(winnerName);

        }
        System.out.println("Benchmark winning ratios (" + numGames + ")");
        for (String name : winLossRecord.getPlayerNames().stream().sorted().collect(Collectors.toList())) {
            System.out.println(name + "\t" + winLossRecord.getWinRatio(name));
        }
        System.out.println();

        return winLossRecord.getWinRatio(nnPlayer.getName());
    }

	private void trainOneGame(long gameSeed) {
		nnPlayer.resetLearning(); // reset eligibility traces

		// TODO remove - just a learning test - play the same game every time
//		HRPlayer opponent = new HRPlayerRandom(98345l);
//		gameSeed = 367l;
//		this.players = Arrays.asList(opponent, nnPlayer);

		// Play against "itself"
		NNHRPlayer opponent = new NNHRPlayer("Opponent", nnPlayer.getNeuralNet());
		opponent.setLearningMode(false);
		this.players = Arrays.asList(opponent, nnPlayer);

		Collections.shuffle(players);
		HRGame game = new HRGame(players, gameSeed);
		game.setLogChanges(false);
		game.play();

		String winnerName = game.getWinner();
		boolean isDraw = false;
		if (winnerName == null) {
		    winnerName = "DRAW";
		    isDraw = true;
		}
		boolean isWinner = nnPlayer.getName().equals(winnerName);
		nnPlayer.learnFinalResult(isWinner, isDraw, game.getState());

		winLossRecord.addWin(winnerName);

//		System.out.println("Ended on turn number " + game.getState().getTurnNumber());
//		System.out.println(game.getState().getTurnNumber());
	}
}
