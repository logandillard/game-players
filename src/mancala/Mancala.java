package mancala;
import games.Game;
import games.InvalidMoveException;
import games.Move;

import java.util.*;

public class Mancala implements Game, Cloneable {
	static final int STORE_INDEX = 6;
	public static final int NUM_TOTAL_MOVES = 6;
	static final int NUM_PITS_PER_ROW = 6;
	static final int NUM_SEEDS_PER_PIT = 4;
	int numSeedsToWin = NUM_PITS_PER_ROW * NUM_SEEDS_PER_PIT + 1;
	
	private boolean isPlayer1Turn;
	private int[] north;
	private int[] south;
	
	Mancala() {
		isPlayer1Turn = true;
		north = new int[NUM_PITS_PER_ROW + 1];	// Player 2's row of pits and store
		south = new int[NUM_PITS_PER_ROW + 1]; // Player 1's row of pits and store
		
		for(int i=0; i<STORE_INDEX; i++) {
			north[i] = NUM_SEEDS_PER_PIT;
			south[i] = NUM_SEEDS_PER_PIT;
		}
	}
	Mancala(Mancala other) {
		isPlayer1Turn = other.isPlayer1Turn;
		north = new int[NUM_PITS_PER_ROW + 1];	// Player 2's row of pits and store
		south = new int[NUM_PITS_PER_ROW + 1]; // Player 1's row of pits and store
		
		for(int i=0; i<NUM_PITS_PER_ROW + 1; i++) {
			north[i] = other.north[i];
			south[i] = other.south[i];
		}
	}
	
	public double evaluate(boolean player1) {
		return materialAdvantage(player1);
		//if(this.isTerminated()) System.out.print("." + ad+".");//System.out.println("Found terminal state - score: " + ad);
	}
	
	private int materialAdvantage(boolean player1) {
		int player1ScoreAdvantage =  south[STORE_INDEX] - north[STORE_INDEX];
		return (player1 ? player1ScoreAdvantage : -player1ScoreAdvantage);
	}

	public boolean isPlayer1Turn() {
		return isPlayer1Turn;
	}

	public boolean isTerminated() {
		return ((getFinalScore(true) >= numSeedsToWin) || (getFinalScore(false) >= numSeedsToWin))|| 
			(sumPits(true) == 0 || sumPits(false) == 0);
	}
	
	private int sumPits(boolean player1) {
		int sum=0;
		if(player1) {
			for(int i=0; i<STORE_INDEX; i++) {
				sum += this.south[i];
			}
		}
		else{
			for(int i=0; i<STORE_INDEX; i++) {
				sum += this.north[i];
			}
		}
		
		return sum;
	}
	
	private int numNonemptyPits(boolean player1) {
		int numNonemptyPits = 0;
		if(player1) {
			for(int i=0; i<STORE_INDEX; i++) {
				if(south[i] > 0) numNonemptyPits++;
			}
		}
		else{
			for(int i=0; i<STORE_INDEX; i++) {
				if(north[i] > 0) numNonemptyPits++;
			}
		}
		
		return numNonemptyPits;
	}
	
	
	public int getNumStonesOnBoard(boolean player1Perspective)
	{
		int stonesOnBoard = 0;
		
		if(player1Perspective) {
			for(int i=0; i<STORE_INDEX; i++) {
				stonesOnBoard += south[i];
			}
		}
		else {
			for(int i=0; i<STORE_INDEX; i++) {
				stonesOnBoard += north[i];
			}
		}
		
		return stonesOnBoard;
	}
	
	

	public int numMoves() {
		return numNonemptyPits(isPlayer1Turn);
	}

	public List<Move> getMoves() {
		List<Move> moves = new ArrayList<Move>();
		
		if(isPlayer1Turn) {
			for(int i=0; i<STORE_INDEX; i++) {
				if(south[i] > 0) moves.add(new MancalaMove(i));
			}
		}
		else{
			for(int i=0; i<STORE_INDEX; i++) {
				if(north[i] > 0) moves.add(new MancalaMove(i + STORE_INDEX));
			}
		}
		
		return moves;
	}

	// Accepts integers between 0 and 11, inclusive.
	// A move for player 1 should be 0-5, for player2 should be 6-11
	public void move(Move o) throws InvalidMoveException {
		// Error checking
		if(!(o instanceof MancalaMove)) throw new InvalidMoveException();
		MancalaMove move = (MancalaMove) o;
		if(move.m < 0 || move.m >= 2*STORE_INDEX) throw new InvalidMoveException();
		if(this.isTerminated()) throw new InvalidMoveException(
				"Game is terminated! No more moves allowed");

		
		// Collect and distribute seeds
		if(move.m < STORE_INDEX) {
			if(!isPlayer1Turn) throw new InvalidMoveException("Wrong side of the board!");
			
			// Collect seeds
			int seeds = south[move.m];
			if(seeds == 0) throw new InvalidMoveException("This pit is empty!");
			south[move.m] = 0;
			
			
			// Distribute seeds
			while(seeds > 0) {
				move.m++;
				if(move.m <= STORE_INDEX) {
					south[move.m]++;
					seeds--;
				}
				else if(move.m <= 2* STORE_INDEX) {
					north[move.m - south.length]++;
					seeds--;
				}
				else {
					move.m = -1;
				}
			}
		}
		else if(move.m >= STORE_INDEX) {
			if(isPlayer1Turn) throw new InvalidMoveException("Wrong side of the board!");
			
			// Collect seeds
			int seeds = north[move.m - STORE_INDEX];
			if(seeds == 0) throw new InvalidMoveException("This pit is empty!");
			north[move.m - STORE_INDEX] = 0;
			
			// Distribute seeds
			while(seeds > 0) {
				move.m++;
				if(move.m < STORE_INDEX) {
					south[move.m]++;
					seeds--;
				}
				else if(move.m <= 2*STORE_INDEX) {
					north[move.m - STORE_INDEX]++;
					seeds--;
				}
				else {
					move.m = -1;
				}
			}
		}
		
		
		// check for special last-seed conditions: 
		// last seed was placed in the store (current player goes again),
		// or last seed was placed in an empty pit, for which the opposite pit was not empty (capture)
		
		// Capture
		if(isPlayer1Turn()) {
			if(move.m < STORE_INDEX) {
				if(south[move.m] == 1 && north[STORE_INDEX-1 - move.m] > 0) {
					int captureNum = south[move.m] + north[STORE_INDEX-1 - move.m];
					south[move.m] = 0;
					north[STORE_INDEX-1 - move.m] = 0;
					south[STORE_INDEX] += captureNum;
				}
			}
		}
		else			
			if(move.m >= STORE_INDEX && move.m < 2*STORE_INDEX) {
				if(north[move.m - STORE_INDEX] == 1 && 
						south[STORE_INDEX - 1 - (move.m - STORE_INDEX)] > 0) {
					int captureNum = north[move.m - STORE_INDEX] + 
						south[STORE_INDEX - 1 - (move.m - STORE_INDEX)];
					north[move.m - STORE_INDEX] = 0;
					south[STORE_INDEX - 1 - (move.m - STORE_INDEX)] = 0;
					north[STORE_INDEX] += captureNum;
				}
			}
		
			
		// Check for one side running out of seeds (ends game with
		// remaining seeds on board going to player whose pits they are in)
		if(sumPits(true) == 0) {
			clearSeeds(false);
		}
		else if(sumPits(false) == 0) {
			clearSeeds(true);
		}
			
		// Change turns if appropriate
		if(((move.m == STORE_INDEX) && isPlayer1Turn) ||
				((move.m == 2*STORE_INDEX) && !isPlayer1Turn)) {
			// Turn remains the same
		}
		else {
			// Change turns
			isPlayer1Turn = !isPlayer1Turn;
		}
	}

	private void clearSeeds(boolean player1Side) {
		int[] pits = (player1Side ? south : north);
		int temp;
		for(int i=0; i<STORE_INDEX; i++) {
			temp = pits[i];
			pits[i] = 0;
			pits[STORE_INDEX] += temp;
		}
	}
	
	public Game clone() {
		return new Mancala(this);
	}

	public double getFinalScore(boolean player1) {
		if(player1) return south[STORE_INDEX];
		else return north[STORE_INDEX];
	}

	public boolean isSuccessor(Game laterGame) {
		throw new UnsupportedOperationException();
	}
	
	public int[] pitsToArray(boolean player1Perspective) {
		int[] out = new int[NUM_PITS_PER_ROW * 2];
		
		if(player1Perspective) {
			for(int i=0; i<NUM_PITS_PER_ROW; i++) {
				out[i] = south[i];
				out[i + NUM_PITS_PER_ROW] = north[i];
			}
		}
		else {
			for(int i=0; i<NUM_PITS_PER_ROW; i++) {
				out[i] = north[i];
				out[i + NUM_PITS_PER_ROW] = south[i];
			}
		}
		
		return out;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		// North numbers
		sb.append("    ");
		for(int i=2*STORE_INDEX; i>STORE_INDEX; i--) {
			sb.append(i + (i>9? "" : " ")+ (i>STORE_INDEX+1 ? "|" : ""));
		}
		
		// North pits
		sb.append("\n    ");
		for(int i=STORE_INDEX-1; i>=0; i--) {
			sb.append(north[i] + "  ");
		}
		
		// Stores
		sb.append("\n" + north[STORE_INDEX] + " ------------------- " + south[STORE_INDEX] + "\n");
		
		// South pits
		sb.append("    ");
		for(int i=0; i<STORE_INDEX; i++) {
			sb.append(south[i] + "  ");
		}
		
		// South numbers
		sb.append("\n    ");
		for(int i=1; i<=STORE_INDEX; i++) {
			sb.append(i + (i<STORE_INDEX? " |" : ""));
		}
		
		
		return sb.toString();
	}
}
