package com.dillard.games.risk;

public interface Game extends Cloneable {

//	void moveCurrentPlayer();

	void play();

	boolean isTerminated();
	
	String getWinner();
	
//	/** Take the specified move */
//	<G extends Game> void move(Move<G> m) throws InvalidMoveException;
//
//	/** Return the number of possible moves for the current turn */
//	int numMoves();
//
//	/** Get all possible moves for the current turn */
//	<G extends Game> List<Move<G>> getMoves();
//
//	/** Has the game completed? */
//	boolean isTerminated();
//
//	/** Is it player 1's turn? */
//	int currentPlayerID();
//
//	<G extends Game> GamePlayer<G> currentPlayer();
//
//	/** Evaluate the result of this game for the specified player */
//	double evaluate(int playerID);
//
//	/** Get the specified player's score. Only valid if the game has completed. */
//	double getFinalScore(int playerID);
//
//	/** Return a clone of this object */
//	Game clone();	
}