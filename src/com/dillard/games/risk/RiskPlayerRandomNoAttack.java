package com.dillard.games.risk;

import java.util.List;
import java.util.Random;

public class RiskPlayerRandomNoAttack extends AbstractGamePlayer implements RiskPlayer {
	private Random rand = new Random(37489234);

	public RiskPlayerRandomNoAttack(String name) {
		super(name);
	}
	
	@Override
	public Move<RiskGame> getMove(
			RiskGameState gameState,
			List<Move<RiskGame>> moves) {
		// Do nothing if possible
		for (Move<RiskGame> move : moves) {
			if (move instanceof MoveNull) {
				return move;
			}
		}
		// otherwise choose randomly
		return moves.get(rand.nextInt(moves.size()));
	}
}
