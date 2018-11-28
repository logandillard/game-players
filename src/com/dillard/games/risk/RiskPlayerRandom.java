package com.dillard.games.risk;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RiskPlayerRandom extends AbstractGamePlayer implements RiskPlayer {
	private Random rand = new Random(37489234);

	public RiskPlayerRandom(String name) {
		super(name);
	}
	
	@Override
	public Move<RiskGame> getMove(
			RiskGameState gameState,
			List<Move<RiskGame>> moves) {

		// TODO this is a hack - probably want to remove it
		// If it's the attack phase and we haven't already taken a territory, then don't take a null move
		if (moves.stream().anyMatch(m -> m instanceof MoveAttack)
				&& !gameState.getCurrentPlayerHasTakenATerritory()) {
			moves = moves.stream().filter(m -> !(m instanceof MoveNull)).collect(Collectors.toList());
		}
		
		return moves.get(rand.nextInt(moves.size()));
	}
}
