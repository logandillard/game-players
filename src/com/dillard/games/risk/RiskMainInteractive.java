package com.dillard.games.risk;

import java.util.ArrayList;
import java.util.List;

public class RiskMainInteractive {
	public static void main(String[] args) {
		System.out.println("STARTING");
		
		RiskUI ui = new RiskUIConsole();

		List<RiskPlayer> players = new ArrayList<>();
		players.add(new RiskPlayerConservativeCardAware("ConservativeCard"));
		players.add(new RiskPlayerConservative("Conservative"));
		players.add(new RiskPlayerAggressive("Aggressive"));
		players.add(new RiskPlayerRandom("Rando"));
		players.add(new RiskPlayerHuman("Human", ui));

		playOneGame(players, false);
		
		System.out.println("DONE");
	}

	private static void playOneGame(List<RiskPlayer> players, boolean logChanges) {
		// start game
		long seed = System.currentTimeMillis();
		System.out.println("Random seed is: " + seed);
		RiskGame game = new RiskGame(players, seed);
		game.setLogChanges(logChanges);
		game.play();

		System.out.println("Winner is: " + game.getWinner());
		System.out.println("Time taken (ms): " + (System.currentTimeMillis() - seed));
	}
}
