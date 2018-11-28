package checkers;

import ioutilities.IOUtilitiesInterface;
import games.ABPruningPlayer;
import games.Game;
import games.GameApp;
import games.GamePlayer;

public class CheckersApp extends GameApp {
	public static void main(String[] args) throws Exception {
		CheckersApp app = new CheckersApp();
		app.run();
	}

	@Override
	protected Game createGame() {
		return new Checkers();
	}

	@Override
	protected GamePlayer getPlayer1() {
		return new HumanCheckersPlayer(iou);
	}
	@Override
	protected GamePlayer getPlayer2() {
		return new ABPruningPlayer<Checkers>(5);
//		return new HumanCheckersPlayer(iou);
	}

	@Override
	protected String getPlayer1Message() {
		return "White's turn";
	}
	@Override
	protected String getPlayer2Message() {
		return "Black's turn";
	}

	@Override
	protected void welcome(IOUtilitiesInterface iou) {
		iou.println("Welcome to checkers\n\n");
	}
}
