package checkers;

import games.ABPruningPlayer;
import checkers.ui.CheckersGUI;

public class CheckersGUIApp {
	public static void main(String[] args) throws Exception {
		Checkers model = new Checkers();
		
		CheckersGUI ui = new CheckersGUI(model, 
				null, 
				new ABPruningPlayer<Checkers>(10));
		ui.run();
	}
}
