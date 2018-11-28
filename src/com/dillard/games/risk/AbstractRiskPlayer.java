package com.dillard.games.risk;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class AbstractRiskPlayer extends AbstractGamePlayer implements RiskPlayer {
	protected final static int NULL_MOVE_SCORE = 0;
	protected Random rand = new Random(37489234);

	public AbstractRiskPlayer(String name) {
		super(name);
	}

	public void setSeed(long seed) {
		rand = new Random(seed);
	}

	@Override
	public Move<RiskGame> getMove(
			RiskGameState gameState,
			List<Move<RiskGame>> moves) {

		double[] moveScores = new double[moves.size()];
		
		for (int i=0; i<moves.size(); i++) {
			Move<RiskGame> move = moves.get(i);
			moveScores[i] = scoreMove(move, gameState);
		}

		double maxScore = -Double.MAX_VALUE;
		int maxIndex = -1;
		for (int i=0; i<moveScores.length; i++) {
			if (moveScores[i] > maxScore) {
				maxScore = moveScores[i];
				maxIndex = i;
			}
		}

		if (maxIndex < 0) {
			// this happens if scores are NaN
			System.out.println("Found no max score in " + Arrays.toString(moveScores) + " for moves " + moves);
		}
		
		return moves.get(maxIndex);
	}

	protected double scoreMove(Move<RiskGame> move, RiskGameState gameState) {
		if (move instanceof MoveAttack) {
			return scoreMove((MoveAttack)move, gameState);
		}
		if (move instanceof MovePlaceArmies) {
			return scoreMove((MovePlaceArmies)move, gameState);
		}
		if (move instanceof MoveUseCards) {
			return scoreMove((MoveUseCards)move, gameState);
		}
		if (move instanceof MoveRegroup) {
			return scoreMove((MoveRegroup)move, gameState);
		}
		if (move instanceof MoveAdvance) {
			return scoreMove((MoveAdvance)move, gameState);
		}
		// move null has a score of zero
		return NULL_MOVE_SCORE;
	}

	protected double scoreMove(MoveAttack move, RiskGameState gameState) {
		return -1;
	}

	protected double scoreMove(MovePlaceArmies move, RiskGameState gameState) {
		return -1;
	}

	protected double scoreMove(MoveRegroup move, RiskGameState gameState) {
		return -1;
	}

	protected double scoreMove(MoveUseCards move, RiskGameState gameState) {
		return -1;
	}

	protected double scoreMove(MoveAdvance move, RiskGameState gameState) {
		return -1;
	}

	protected double moveTowardPotentialConflict(MoveRegroup move, RiskGameState state) {
		// move toward more potential conflict
		int[] fromOpposingCounts = countOpposingAdjacentArmies(move.getFrom(), state);
		int[] toOpposingCounts = countOpposingAdjacentArmies(move.getTo(), state);

		if (toOpposingCounts[0] != 0 || fromOpposingCounts[0] != 0) {
			if (toOpposingCounts[1] != fromOpposingCounts[1]) {
				// Difference in opposing armies. High priority.
				double opposingAdvantageToMove = 
						toOpposingCounts[1] - (fromOpposingCounts[1] + 0.5); // -0.5 to not move if there is no advantage
				if (opposingAdvantageToMove > 0) {
					return opposingAdvantageToMove + 100_000;
				} else {
					return opposingAdvantageToMove - 100_000;
				}
			}

			// Same threat level. spread out armies defensively. Medium priority
			// need an advantage of > 1 to move, or we can move in infinite cycles
			double spreadOutAdvantageToMove = move.getFrom().getArmyCount() - move.getTo().getArmyCount() -1.5;
			if (spreadOutAdvantageToMove > 0) {
				return spreadOutAdvantageToMove + 1000;
			} else {
				return spreadOutAdvantageToMove - 1000;
			}
		} else { // Neither territory has opposing adjacent armies. These moves are low priority.
			// Difference in distance to enemy territory
			return distanceToOpposingAdjacentTerritory(move.getFrom(), state) - 
					distanceToOpposingAdjacentTerritory(move.getTo(), state) -0.5;
		}
	}

	/** Returns my army count - the opposing adjacent territories' army count */
	protected double scoreMoveForAdjacentDefense(MovePlaceArmies move, RiskGameState state) {
		int[] opposingCounts = countOpposingAdjacentArmies(move.getTerritory(), state);

		// if this territory is surrounded by my own territories, don't focus on it
		if (opposingCounts[0] == 0) {
			return -Integer.MAX_VALUE;
		}

		// put armies where I'm weakest
		return opposingCounts[1] - state.getTerritoryState(move.getTerritory()).getArmyCount();
	}

	protected double adjacentAdvantage(Territory territory, RiskGameState state) {		
		int[] opposingCounts = countOpposingAdjacentArmies(territory, state);
		return state.getTerritoryState(territory).getArmyCount() - opposingCounts[1];
	}

	protected double scoreMoveToTakeContinent(MovePlaceArmies move, RiskGameState state) {
		return scoreMoveToTakeContinent(move.getTerritory(), move.getArmiesRemaining(), state);
	}

	protected double scoreMoveToTakeContinent(Territory territory, int armiesRemaining, RiskGameState state) {
		int[] opposingCounts = countOpposingAdjacentArmies(territory, state);

		// if this territory is surrounded by my own territories, don't focus on it
		if (opposingCounts[0] == 0) {
			return -Integer.MAX_VALUE;
		}

		// put armies where I could take a continent
		int[] opposingCountsInContinent = opposingArmiesInContinent(territory.getContinent(), state);

		// if no one else owns territories in this continent, don't focus on it
		if (opposingCountsInContinent[0] == 0) {
			return -Integer.MAX_VALUE/2;
		}

		int[] myCountsInContinent = myArmiesInContinent(territory.getContinent(), state);

		return myCountsInContinent[1] + armiesRemaining - opposingCountsInContinent[1];
	}

	/** Returns the number of armies I have in the continent minus the number that are not mine in the continent */
	protected double continentAdvantage(Territory territory, RiskGameState state) {
		int[] opposingCountsInContinent = opposingArmiesInContinent(territory.getContinent(), state);
		int[] myCountsInContinent = myArmiesInContinent(territory.getContinent(), state);

		return myCountsInContinent[1] - opposingCountsInContinent[1];
	}

	protected int[] opposingArmiesInContinent(Territory t, RiskGameState state) {
		return countArmiesInContinent(t.getContinent(), state, true);
	}

	protected int[] opposingArmiesInContinent(Continent continent, RiskGameState state) {
		return countArmiesInContinent(continent, state, true);
	}

	protected final int[] myArmiesInContinent(Territory t, RiskGameState state) {
		return myArmiesInContinent(t.getContinent(), state);
	}

	protected final int[] myArmiesInContinent(Continent continent, RiskGameState state) {
		return countArmiesInContinent(continent, state, false);
	}

	private final int[] countArmiesInContinent(Continent continent, RiskGameState state, boolean opposingPlayers) {
		int[] counts = new int[2];
		for (Territory t : continent.getTerritoryList()) {
			TerritoryState ts = state.getTerritoryState(t);
			if (isMe(ts.getOwner()) != opposingPlayers) {
				counts[0]++;
				// +1 because a territory can still be owned with 0 armies
				counts[1] += ts.getArmyCount() + 1;
			}
		}
		return counts;
	}

	protected final int[] countOpposingAdjacentArmies(TerritoryState ts, RiskGameState state) {
		return countOpposingAdjacentArmies(ts.getTerritory(), state);
	}

	protected final int[] countOpposingAdjacentArmies(Territory terr, RiskGameState state) {
		int[] counts = new int[2];
		for (Territory t : terr.getAdjacentTerritories()) {
			if (!isMe(state.getTerritoryState(t).getOwner())) {
				counts[0]++;
				// +1 because a territory can still be owned with 0 armies
				counts[1] += state.getTerritoryState(t).getArmyCount() + 1;
			}
		}
		return counts;
	}

	protected boolean hasOpposingAdjacentTerritory(Territory terr, RiskGameState state) {
		return terr.getAdjacentTerritories().stream().anyMatch(t -> !isMe(state.getTerritoryState(t).getOwner()));
	}

	protected final int distanceToOpposingAdjacentTerritory(TerritoryState ts, RiskGameState state) {
		return state.distanceToOpposingAdjacentTerritory(ts.getTerritory());
	}

	protected boolean isMe(RiskPlayer player) {
		return player == this;
	}
}
