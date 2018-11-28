package com.dillard.games.risk;

import java.util.List;
import java.util.Random;

public class RiskPlayerPartiallyLazy extends AbstractGamePlayer implements RiskPlayer {
	private Random rand = new Random(37489234);
	private double lazyProbability;
	
	public RiskPlayerPartiallyLazy(String name, double lazyProbability) {
		super(name);
		this.lazyProbability = lazyProbability;
	}
	
	@Override
	public Move<RiskGame> getMove(
			RiskGameState gameState,
			List<Move<RiskGame>> moves) {
		// Do nothing if possible
	    if (rand.nextDouble() < lazyProbability) {
    		for (Move<RiskGame> move : moves) {
    			if (move instanceof MoveNull) {
    				return move;
    			}
    		}
	    }
		// otherwise choose randomly
		return moves.get(rand.nextInt(moves.size()));
	}
}
