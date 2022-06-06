package com.dillard.games.mancala;

import java.util.Random;

import com.dillard.games.ABPruningPlayer;
import com.dillard.games.ConsoleIOUtilities;
import com.dillard.games.GamePlayer;
import com.dillard.games.HumanPlayer;
import com.dillard.games.IOUtilitiesInterface;
import com.dillard.games.InvalidMoveException;
import com.dillard.games.RandomPlayer;


public class MancalaApp {
	public static final String MODEL_FILE_NAME = NNMancalaPlayer.MODEL_FILE;

	public static void main(String args[]) throws Exception {
		IOUtilitiesInterface iou = new ConsoleIOUtilities();

		iou.println("Welcome to Mancala");

		String compPlayerMenu = "Who will play the game?\n" +
				"1. Human vs. Random\n" +
				"2. Human vs. Neural Network\n" +
				"3. Human vs. A-B pruning\n" +
				"4. Two human players\n" +
				"5. Random vs. A-B pruning\n" +
				"6. Random vs. Neural Network\n" +
				"7. A-B Pruning vs. Neural Network\n" +
				"8. Neural network vs. self\n";

		int compPlayerChoice = iou.getInt(compPlayerMenu, 1, 8);
		GamePlayer player1 = null;
		GamePlayer player2 = null;
		String player1Msg = "";
		String player2Msg = "";
		int depth;

		switch(compPlayerChoice) {
		case 1:
			iou.println("Playing against a random player.");
			player1 = new HumanMancalaPlayer(1, Mancala.NUM_TOTAL_MOVES, -1, iou);
			player2 = new RandomPlayer();
			player1Msg = "Your turn...";
			player2Msg = "Random player's turn.";
			break;
		case 2:
			iou.println("Playing against a neural network player.");
			player1 = new HumanMancalaPlayer(1, Mancala.NUM_TOTAL_MOVES, -1, iou);
			player2 = new NNMancalaPlayer(MODEL_FILE_NAME);
			player1Msg = "Your turn...";
			player2Msg = "Neural network player's turn.";
			break;
		case 3:
			depth = iou.getInt("What depth should the player look ahead to? ", 1, 9);
			iou.println("Playing against an alpha beta pruning minimax player - Depth=" + depth);
			player1 = new HumanMancalaPlayer(1, Mancala.NUM_TOTAL_MOVES, -1, iou);
			player2 = new ABPruningPlayer(depth, new Random(342345));
			player1Msg = "Your turn...";
			player2Msg = "AB pruning player's turn.";
			break;
		case 4:
			iou.println("Playing with two human players.");
			player1 = new HumanMancalaPlayer(1, Mancala.NUM_TOTAL_MOVES, -1, iou);
			player2 = new HumanMancalaPlayer(Mancala.NUM_TOTAL_MOVES+1, 2*Mancala.NUM_TOTAL_MOVES, -1, iou);
			player1Msg = "Human 1 turn.";
			player2Msg = "Human 2 turn.";
			break;
		case 5:
			depth = iou.getInt("What depth should the AB-pruning player look ahead to? ", 1, 17);
			iou.println("Random player playing against an alpha beta pruning minimax player - Depth=" + depth);
			player1 = new RandomPlayer();
			player2 = new ABPruningPlayer(depth, new Random(342345));
			player1Msg = "Random player's turn.";
			player2Msg = "AB pruning player's turn.";
			break;
		case 6:
			player1 = new RandomPlayer();
			player2 = new NNMancalaPlayer(MODEL_FILE_NAME);
			player1Msg = "Random player's turn.";
			player2Msg = "Neural network player's turn.";
			break;
		case 7:
			depth = iou.getInt("What depth should the AB-pruning player look ahead to? ", 1, 17);
			iou.println("Alpha beta pruning minimax player - Depth=" + depth + " playing against NN player");
			player1 = new ABPruningPlayer(depth, new Random(342345));
			player2 = new NNMancalaPlayer(MODEL_FILE_NAME);
			player1Msg = "ABpruning player's turn.";
			player2Msg = "Neural network player's turn.";
			break;
		case 8:
			player1 = new NNMancalaPlayer(MODEL_FILE_NAME);
			player2 = new NNMancalaPlayer(MODEL_FILE_NAME);
			player1Msg = "NN player 1's turn.";
			player2Msg = "NN player 2's turn.";
			break;
		}


//		boolean humanFirst = iou.getYesNo("Do you want to go first? ");
		//iou.println("\nYou go first.");
		Mancala game = new Mancala();
//		GamePlayer player1 = new HumanPlayer(1, Mancala.NUM_TOTAL_MOVES, iou);
//		GamePlayer player1 = new RandomPlayer();

		MancalaMove move;
//		String prompt;

		iou.println(game.toString() + "\n");

		while(!game.isTerminated()) {

			// If it is the human player's turn
			if(game.isPlayer1Turn()) {
				iou.println(player1Msg);
				// Get the next move from the player
				if(player1 instanceof HumanPlayer) {
					move = (MancalaMove)((HumanPlayer)player1).move(game, false);
				}
				else {
					move = (MancalaMove)player1.move(game);
				}
			}
			else { // Computer player's turn
				iou.println(player2Msg);
				move = (MancalaMove) player2.move(game);
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

}
