package com.dillard.games.risk;

import java.util.ArrayList;
import java.util.List;

public class RiskGameTimeTest {
	public static void main(String[] args) {
		System.out.println("STARTING");

		List<RiskPlayer> players = new ArrayList<>();
		players.add(new RiskPlayerConservativeCardAware("ConservativeCard"));
		players.add(new RiskPlayerConservative("Conservative"));
		players.add(new RiskPlayerAggressive("Aggressive"));
		players.add(new RiskPlayerRandom("Rando"));

		long numGames = 100;
		long start = System.currentTimeMillis();
		for (int i=0; i<numGames; i++) {
			playOneGame(players, false);
		}
		System.out.println("Time taken per game (ms): " + ((System.currentTimeMillis() - start)/numGames));
		
		System.out.println("DONE");
	}

	private static void playOneGame(List<RiskPlayer> players, boolean logChanges) {
		// start game
		long seed = System.currentTimeMillis();
		RiskGame game = new RiskGame(players, seed);
		game.setLogChanges(logChanges);
		game.play();
	}
}
