package com.dillard.games;

import java.util.List;

public class RandomPlayer<M extends Move, G extends Game<M, G>> implements GamePlayer<M, G> {

	public M move(G theGame) {
		List<M> moves = theGame.getMoves();
		return moves.get((int)(Math.random() * moves.size()));
	}

}
