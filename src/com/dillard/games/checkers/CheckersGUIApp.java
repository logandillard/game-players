package com.dillard.games.checkers;

import com.dillard.games.ABPruningPlayer;
import com.dillard.games.checkers.ui.CheckersGUI;

public class CheckersGUIApp {
	public static void main(String[] args) throws Exception {
		CheckersGame model = new CheckersGame();

		CheckersGUI ui = new CheckersGUI(model,
				null,
				new ABPruningPlayer<CheckersGame>(10));
		ui.run();
	}
}
