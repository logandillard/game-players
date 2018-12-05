package com.dillard.games;

public abstract class GameApp {
	protected IOUtilitiesInterface iou = new ConsoleIOUtilities();

	public void run() throws Exception {
		welcome(iou);

		GamePlayer player1 = getPlayer1();
		GamePlayer player2 = getPlayer2();
		String player1Message = getPlayer1Message();
		String player2Message = getPlayer2Message();

		Game game = createGame();

		Move move;

		iou.println(game.toString() + "\n");

		while(!game.isTerminated()) {

			// If it is the human player's turn
			if(game.isPlayer1Turn()) {
				iou.println(player1Message);
				// Get the next move from the player
				if(player1 instanceof HumanPlayer) {
					move = ((HumanPlayer)player1).move(game, false);
				}
				else {
					move = player1.move(game);
				}
			} else { // Computer player's turn
				iou.println(player2Message);
				move = player2.move(game);
			}

			// Make the move
			try {
				game.move(move);
			} catch(InvalidMoveException exc) {
				iou.println("That was not a valid move... try again.\n");
			}

			// Print the game board
			iou.println("\n\n" + game.toString() + "\n");
		}

		double p1Score = game.getFinalScore(true);
		double p2Score = game.getFinalScore(false);

		if(p1Score > p2Score) {
			if(player1 instanceof HumanPlayer)
				System.out.println("You win!!");
			else
				System.out.println("Player 1 wins!!");
		}
		else if(p2Score > p1Score) {
			if(player1 instanceof HumanPlayer)
				System.out.println("Computer player wins!!");
			else
				System.out.println("Player 2 wins!!");
		}
		else {
			System.out.println("Tie game!!");
		}
	}


	protected abstract Game createGame() ;

	protected abstract String getPlayer1Message();
	protected abstract String getPlayer2Message();

	protected abstract GamePlayer getPlayer2() ;
	protected abstract GamePlayer getPlayer1() ;
	protected abstract void welcome(IOUtilitiesInterface iou) ;
}
