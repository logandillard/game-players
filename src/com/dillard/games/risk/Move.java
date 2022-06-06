package com.dillard.games.risk;

public interface Move<G extends Game> {
	void execute(G game);
}
