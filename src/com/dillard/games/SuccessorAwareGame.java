package com.dillard.games;

public interface SuccessorAwareGame extends Game {
	
	/** Can the specified later game be reached from this game state? */
	boolean isSuccessor(Game laterGame);
}
