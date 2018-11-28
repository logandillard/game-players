package com.dillard.games.risk;

public class RiskPlayerAggressive extends AbstractRiskPlayer implements RiskPlayer {

	public RiskPlayerAggressive(String name) {
		super(name);
	}

	@Override
	protected double scoreMove(MoveAttack move, RiskGameState state) {
		// if we haven't taken a territory yet, then try really hard to take one
		int noNewCardBonus = state.getCurrentPlayerHasTakenATerritory() ? 0 : 10_000_000;

		// attack whoever we can kill!
		return noNewCardBonus + move.getFrom().getArmyCount() -2 - move.getTo().getArmyCount();
	}

	@Override
	protected double scoreMove(MovePlaceArmies move, RiskGameState state) {
		double takeContinent = scoreMoveToTakeContinent(move, state);
		if (takeContinent > 0) {
			return takeContinent + 100_000;
		}		
		return scoreMoveForAdjacentDefense(move, state);
	}

	@Override
	protected double scoreMove(MoveRegroup move, RiskGameState state) {
		double fromTakeContinent = scoreMoveToTakeContinent(move.getFrom().getTerritory(), move.getArmiesRemaining(), state);
		double toTakeContinent = scoreMoveToTakeContinent(move.getTo().getTerritory(), move.getArmiesRemaining(), state);

		if (toTakeContinent > 0 || fromTakeContinent > 0) {
			if (toTakeContinent > fromTakeContinent) {
				return toTakeContinent + 100_000_000 - fromTakeContinent; // Always place armies to try to take a continent
			} else if (fromTakeContinent >= toTakeContinent) {
				return toTakeContinent - 100_000_000 - fromTakeContinent;
			}
			// TODO if they're the same but both > 0, could pick a winner based on which continent it is
		}

		return moveTowardPotentialConflict(move, state);
	}

	@Override
	protected double scoreMove(MoveAdvance move, RiskGameState state) {
		double fromTakeContinent = scoreMoveToTakeContinent(move.getFrom().getTerritory(), move.getArmiesRemaining(), state);
		double toTakeContinent = scoreMoveToTakeContinent(move.getTo().getTerritory(), move.getArmiesRemaining(), state);
		return toTakeContinent - fromTakeContinent - 0.5; // always just care about taking continents
	}

	protected double scoreMove(MoveUseCards move, RiskGameState gameState) {
		// always use cards
		return NULL_MOVE_SCORE + 1;
	}
}
