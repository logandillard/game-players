package com.dillard.games.risk;

public class RiskPlayerConservativeCardAware extends AbstractRiskPlayer implements RiskPlayer {

	public RiskPlayerConservativeCardAware(String name) {
		super(name);
	}

	@Override
	protected double scoreMove(MoveAttack move, RiskGameState state) {
		TerritoryState toState = move.getTo();

		int[] opposingArmiesInContinentCounts = opposingArmiesInContinent(toState.getTerritory().getContinent(), state);
		int myArmies = move.getFrom().getArmyCount();

		// only attack if I won't yet get a card, or I have more armies than everyone else in the continent
		if (state.getCurrentPlayerHasTakenATerritory() &&
				myArmies - (opposingArmiesInContinentCounts[1]) <= 0) return -1;

		// attack from the strongest position
		return myArmies -2 - toState.getArmyCount();
	}

	@Override
	protected double scoreMove(MovePlaceArmies move, RiskGameState state) {
		double continentAdvantage = scoreMoveToTakeContinent(move, state);
		if (continentAdvantage > opposingArmiesInContinent(move.getTerritory(), state)[0]) {
			return continentAdvantage + 100_000;
		}
		return scoreMoveForAdjacentDefense(move, state);
	}

	@Override
	protected double scoreMove(MoveRegroup move, RiskGameState state) {
		// TODO I should only protect territories that I care about - own the continent or want to
		return moveTowardPotentialConflict(move, state);
	}

	@Override
	protected double scoreMove(MoveAdvance move, RiskGameState state) {
		double fromContinentAd = continentAdvantage(move.getFrom().getTerritory(), state);
		double toContinentAd = continentAdvantage(move.getTo().getTerritory(), state);

		if (toContinentAd > fromContinentAd && toContinentAd > 0) {
			return toContinentAd + 100_000; // Always place armies to try to take a continent
		}

		// this order is on purpose - only move to if it has fewer armies
		return adjacentAdvantage(move.getFrom().getTerritory(), state) -
				adjacentAdvantage(move.getTo().getTerritory(), state);
	}

	protected double scoreMove(MoveUseCards move, RiskGameState state) {
		// always use cards
		return NULL_MOVE_SCORE + 1;
	}
}
