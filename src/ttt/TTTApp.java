package ttt;
import games.ABPruningPlayer;
import games.GamePlayer;
import games.InvalidMoveException;
import games.MinimaxPlayer;
import games.Move;
import games.RandomPlayer;
import ioutilities.ConsoleIOUtilities;
import ioutilities.IOUtilities;

public class TTTApp {
	public static void main(String args[]) throws Exception {
		IOUtilities iou = new ConsoleIOUtilities();

		iou.println("Welcome to Tic-Tac-Toe");
		
		String compPlayerMenu = "What kind of player do you want to play against?\n" + 
				"1. Random\n" + 
				"2. Minimax\n" +
				"3. A-B pruning\n";
		
		int compPlayerChoice = iou.getInt(compPlayerMenu, 1, 3);
		GamePlayer compPlayer = null;
		switch(compPlayerChoice) {
		case 1:
			iou.println("Playing against a random player.");
			compPlayer = new RandomPlayer();
			break;
		case 2:
			iou.println("Playing against a minimax player.");
			compPlayer = new MinimaxPlayer();
			break;
		case 3:
			int depth = iou.getInt("What depth should the player look ahead to? ", 1, 9);
			iou.println("Playing against an alpha beta pruning minimax player - Depth=" + depth);
			compPlayer = new ABPruningPlayer(depth);
			break;	
		}

		
		boolean humanFirst = iou.getYesNo("Do you want to go first? ");
		TicTacToe game = new TicTacToe(humanFirst);

		
		TTTMove move;
		String prompt;
		
		iou.println("You are X's\n\nThis is the game board:\n");
		iou.println(game.toString());
		
		while(!game.isTerminated()) {
			
			// If it is the human player's turn
			if(game.isXTurn()) {
				// Get the next move from the player
//				prompt = (game.isXTurn()? "X's move..." : "O's move...");
				move = new TTTMove( iou.getInt("Your turn (X)...", 1, TicTacToe.NUM_TOTAL_PLACES) );
				
				// Change the input move (keypad) into the game board's numbering (upside-down keypad)
				if(move.m < 4) {
					move.m += 6;
				}
				else if(move.m > 6) {
					move.m -= 6;
				}
			}
			else { // Computer player's turn
				iou.println("Computer player's turn (O)...");
				move = (TTTMove) compPlayer.move(game);
			}
			
			// Make the move
			try {
				game.move(move);
			} catch(InvalidMoveException exc) {
				iou.println("That was not a valid move... try again.\n");
			}
			
			// Print the game board
			iou.println(game.toString());
		}
		
		int winner = game.winner();
		switch(winner) {
		case TicTacToe.X_VALUE:
			iou.println("X wins!");
			break;
		case TicTacToe.O_VALUE:
			iou.println("O wins!");
			break;
		case TicTacToe.DEFAULT_VALUE:
			iou.println("Cat's game!");
			break;
		}
	}
}
