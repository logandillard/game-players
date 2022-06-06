package com.dillard.games.checkers;

import java.util.Random;

import com.dillard.games.ABPruningPlayer;
import com.dillard.games.checkers.ui.CheckersGUI;

public class CheckersGUIApp {
	public static void main(String[] args) throws Exception {
		CheckersGame model = new CheckersGame();

		CheckersGUI ui = new CheckersGUI(model,
				null,
				new ABPruningPlayer<CheckersMove, CheckersGame>(10, new Random(524353)));
		ui.run();
	}
}
