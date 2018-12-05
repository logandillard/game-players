package com.dillard.games.mancala;

import com.dillard.games.ABNNPlayer;
import com.dillard.games.ABPruningPlayer;
import com.dillard.games.GamePlayer;
import com.dillard.games.GamePlayerEvaluator;
import com.dillard.games.RandomPlayer;

public class MancalaPlayerEvaluator {
	public static void main(String[] args) throws Exception {

		ABNNPlayer player = new ABNNMancalaPlayer(3, NNMancalaPlayer.MODEL_FILE);
		GamePlayer randPlayer = new RandomPlayer();
		GamePlayer abPruning1 = new ABPruningPlayer(1);
		GamePlayer abPruning2 = new ABPruningPlayer(2);
		GamePlayer abPruning3 = new ABPruningPlayer(3);
		GamePlayer abPruning4 = new ABPruningPlayer(4);

		GamePlayer[] benchOpponents = new GamePlayer[] {abPruning4, abPruning3, abPruning2, abPruning1};
		String[] benchOpponentsNames = new String[] {"AB-4", "AB-3","AB-2", "AB-1"};
		Mancala mancala = new Mancala();

		GamePlayerEvaluator.evaluate(mancala, player, benchOpponents, benchOpponentsNames);
	}
}
