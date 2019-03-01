package com.dillard.games;

import java.util.List;

public interface Game<M extends Move, G extends Game<M, G>> extends Cloneable {

	/** Take the specified move */
	void move(M m) throws InvalidMoveException;

	/** Return the number of possible moves for the current turn */
//	int numMoves();

	/** Get all possible moves for the current turn */
	List<M> getMoves();

	/** Has the game completed? */
	boolean isTerminated();

	/** Is it player 1's turn? */
	boolean isPlayer1Turn();

	/** Evaluate the result of this game for the specified player */
	double evaluate(boolean player1);

	/** Get the specified player's score. Only valid if the game has completed. */
	double getFinalScore(boolean player1);

	boolean equals(Object o);

	/** Return a clone of this object */
	G clone();
}
