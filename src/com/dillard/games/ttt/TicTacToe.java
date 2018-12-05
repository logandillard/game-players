package com.dillard.games.ttt;

import java.util.ArrayList;
import java.util.List;

import com.dillard.games.ConsoleIOUtilities;
import com.dillard.games.Game;
import com.dillard.games.IOUtilities;
import com.dillard.games.InvalidMoveException;
import com.dillard.games.Move;

public class TicTacToe implements Game {
	public static final byte DEFAULT_VALUE = 0;
	public static final byte X_VALUE = 1;
	public static final byte O_VALUE = 2;
	public static final int NUM_TOTAL_PLACES = 9;
	private static final int NUM_ROWS = 3;
	private static final int NUM_COLS = 3;

	private int numEmptyPlaces;
	private boolean xTurn;
	private byte[] board;

	// Constructors
	TicTacToe() {
		xTurn = true;
		board = new byte[NUM_TOTAL_PLACES];
		for(int i=0; i<NUM_TOTAL_PLACES; i++) {
			board[i] = DEFAULT_VALUE;
		}
		numEmptyPlaces = NUM_TOTAL_PLACES;
	}
	TicTacToe(boolean xFirst) {
		xTurn = xFirst;
		board = new byte[NUM_TOTAL_PLACES];
		for(int i=0; i<NUM_TOTAL_PLACES; i++) {
			board[i] = DEFAULT_VALUE;
		}
		numEmptyPlaces = NUM_TOTAL_PLACES;
	}
	TicTacToe(TicTacToe other) {
		xTurn = other.xTurn;
		board = new byte[NUM_TOTAL_PLACES];
		for(int i=0; i<NUM_TOTAL_PLACES; i++) {
			board[i] = other.board[i];
		}
		numEmptyPlaces = other.numEmptyPlaces;
	}

	@Override
    public TicTacToe clone() {
		return new TicTacToe(this);
	}


	// Game logic methods
	public void move(Move o) throws InvalidMoveException {
		if(!(o instanceof TTTMove)) {
			throw new InvalidMoveException("Move must be a TTTMove");
		}
		int place = ((TTTMove) o).m;
		if(place < 1 || place > NUM_TOTAL_PLACES) {
			throw new InvalidMoveException("Move out of bounds");
		}
		place--;
		if(board[place] != DEFAULT_VALUE) {
			throw new InvalidMoveException("Board position is not open");
		}

		board[place] = (isXTurn() ? X_VALUE: O_VALUE);
		numEmptyPlaces--;

		changeTurns();
	}

	private void changeTurns() {
		xTurn = !xTurn;
	}

	public boolean isXTurn() {
		return xTurn;
	}

	public boolean isPlayer1Turn() {
		return isXTurn();
	}





	// Utility methods
	private int x(int n) {
		return player(true, n);
	}
	private int o(int n) {
		return player(false, n);
	}

	private int player(boolean isX, int n) {
		if(n < 1 || n > NUM_ROWS) {
			throw new IllegalArgumentException();
		}

		int matches = 0;
		int numX, numO;
		// For each row, col, diagonal
		// If it has n x's and 0 o's, matches++
		// Rows
		for(int i=0; i<NUM_ROWS; i++) {
			numX = numO = 0;
			for(int j=0; j<NUM_COLS; j++) {

				switch(board[NUM_COLS*i + j]) {
				case X_VALUE:
					numX++;
					break;
				case O_VALUE:
					numO++;
					break;
				}
			}
			if(isMatch(isX, n, numX, numO)) matches++;
		}

		// Cols
		for(int i=0; i<NUM_COLS; i++) {
			numX = numO = 0;
			for(int j=0; j<NUM_ROWS; j++) {

				switch(board[NUM_COLS*j + i]) {
				case X_VALUE:
					numX++;
					break;
				case O_VALUE:
					numO++;
					break;
				}
			}
			if(isMatch(isX, n, numX, numO)) matches++;
		}

		// Upper-Left to lower-right diagonal
		numX = numO = 0;
		for(int i=0; i<NUM_COLS; i++) {
			int j=i;

			switch(board[NUM_COLS*i + j]) {
			case X_VALUE:
				numX++;
				break;
			case O_VALUE:
				numO++;
				break;
			}
		}
		if(isMatch(isX, n, numX, numO)) matches++;

		// Upper-right to lower-left diagonal
		numX = numO = 0;
		for(int i=0; i<NUM_COLS; i++) {
			int j=NUM_COLS-i-1;

			switch(board[NUM_COLS*i + j]) {
			case X_VALUE:
				numX++;
				break;
			case O_VALUE:
				numO++;
				break;
			}
		}
		if(isMatch(isX, n, numX, numO)) matches++;

		return matches;
	}

	private boolean isMatch(boolean isX, int n, int numX, int numO){
		if(isX) {
			if(numX == n && numO == 0) {
				return true;
			}
		}
		else {
			if(numO == n && numX == 0) {
				return true;
			}
		}
		return false;
	}


	public boolean isTerminated() {
		// If the board is filled
		if(numEmptyPlaces() == 0) {
			return true;
		}

		// If a player has won
		else if(x(NUM_ROWS) > 0 || o(NUM_ROWS) > 0) {
			return true;
		}

		return false;
	}

	public int winner() {
		if(!isTerminated()) {
			return DEFAULT_VALUE;
		}
		if(x(3) > 0) {
			return X_VALUE;
		}
		if(o(3) > 0) {
			return O_VALUE;
		}

		return DEFAULT_VALUE;
	}

	public double getFinalScore(boolean playerx) {
		int winner = winner();
		int score;

		switch(winner) {
		case X_VALUE:
			score = 1;
			break;
		case O_VALUE:
			score = -1;
			break;
		default:
			score = 0;
		}

		if(playerx) return score;
		else return -score;
	}

	public int numEmptyPlaces(){
		return numEmptyPlaces;
	}

	public int numMoves() {
		return numEmptyPlaces();
	}




	// Game Player methods
	public double evaluate(boolean playerx) {
		if(isTerminated()) {
			if(x(3) > 0) return 10 * (playerx? 1 : -1);
			else if(o(3) > 0) return 10 * (playerx? -1 : 1);
			else return 0;
		}

		double score = 3*x(2) + x(1) - (3*o(2) + o(1));
		if(playerx) {
			return score;
		}
		else return -score;
	}

	public boolean isSuccessor(Game earlierState) {
		if(!(earlierState instanceof TicTacToe)) throw new ClassCastException();
		TicTacToe earlierGame = ((TicTacToe) earlierState).clone();

		// If there exists a combination of moves that will make
		// this become laterGame, return true

		boolean altered;
		boolean everAltered = false;

		do{
			altered = false;
			for(int i=0; i<NUM_TOTAL_PLACES; i++) {
				// If the boards differ
				if(this.board[i] != earlierGame.board[i]) {
					// The earlier board must be empty
					if(earlierGame.board[i] != DEFAULT_VALUE) {
						return false;
					}

					// Make the move if it is the right turn
					if(earlierGame.isXTurn() == (this.board[i] == X_VALUE)) {
						try {
							earlierGame.move(new TTTMove(i+1));
							altered = true;
							everAltered = true;
						} catch(InvalidMoveException exc) {
							throw new RuntimeException();
						}
					}
				}
			}
		} while(altered);

		// return true if this game is now equal to the later game
		// ** note, this game could have started out equal to the later game
		return this.equals(earlierGame);
	}

	public List<Move> getMoves() {
		List<Move> moves = new ArrayList<Move>();
//		int[] moves = new int[NUM_TOTAL_PLACES];
//		int i=0;

		for(int j=0; j<NUM_TOTAL_PLACES; j++) {
			if(board[j] == DEFAULT_VALUE) {
				moves.add(new TTTMove(j + 1));
			}
		}

		return moves;
	}

	// The game's display method
	@Override
    public String toString() {
		StringBuffer s = new StringBuffer();
		for(int j=0; j<NUM_ROWS; j++) {
			for(int i=0; i<NUM_COLS; i++) {

				switch(board[NUM_COLS*j+i]) {
					case X_VALUE:
						s.append('X');
						break;
					case O_VALUE:
						s.append('O');
						break;
					case DEFAULT_VALUE:
						s.append(' ');
				}

				if(i < NUM_COLS-1) s.append(" | ");
			}

			s.append('\n');
		}

		return s.toString();
	}


	@Override
    public boolean equals(Object o) {
		if(!(o instanceof TicTacToe)) {
			throw new ClassCastException();
		}
		TicTacToe other = (TicTacToe) o;

		// Check that the board is the same
		for(int i=0; i<board.length; i++) {
			if(board[i] != other.board[i]) {
				return false;
			}
		}
		// Check that it is the same player's turn
		if(!(isXTurn() == other.isXTurn())) {
			return false;
		}

		return true;
	}

	// A driver for the game (two human players)
	public static void main(String args[]) {
		IOUtilities iou = new ConsoleIOUtilities();
		TicTacToe game = new TicTacToe();
		TTTMove move;
		String prompt;

		iou.println("Welcome to Tic-Tac-Toe\nThis is the game board:\n");
		iou.println(game.toString());

		while(!game.isTerminated()) {
			// Get the next move from the player
			prompt = (game.isXTurn()? "X's move..." : "O's move...");
			move = new TTTMove( iou.getInt(prompt, 1, NUM_TOTAL_PLACES) );

			// Change the input move (keypad) into the game board's numbering (upside-down keypad)
			if(move.m < 4) {
				move.m += 6;
			}
			else if(move.m > 6) {
				move.m -= 6;
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
		case X_VALUE:
			iou.println("X wins!");
			break;
		case O_VALUE:
			iou.println("O wins!");
			break;
		case DEFAULT_VALUE:
			iou.println("Cat's game!");
			break;
		}
	}
}
