package com.dillard.games;

public interface GamePlayer<M extends Move, G extends Game<M, G>> {
	M move(G theGame) throws Exception ;
}
