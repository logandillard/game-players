package com.dillard.games.risk;

public class MovePlaceArmies extends RiskMove implements Move<RiskGame> {

	public final Territory territory;
	public final int armiesRemaining;
	public final int territoryArmyCount;
	
	public MovePlaceArmies(Territory territory, int territoryArmyCount, int armiesRemaining) {
		super(TurnPhase.PLACE_ARMIES);
		this.territory = territory;
		this.territoryArmyCount = territoryArmyCount;
		this.armiesRemaining = armiesRemaining;
	}

	public void execute(RiskGame game) {
		game.applyMove(this);
	}

	public String toString() {
		return territory + " (" + territoryArmyCount + ") remaining " + armiesRemaining;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + armiesRemaining;
		result = prime * result
				+ ((territory == null) ? 0 : territory.hashCode());
		result = prime * result + territoryArmyCount;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MovePlaceArmies other = (MovePlaceArmies) obj;
		if (armiesRemaining != other.armiesRemaining)
			return false;
		if (territory != other.territory)
			return false;
		if (territoryArmyCount != other.territoryArmyCount)
			return false;
		return true;
	}

	public Territory getTerritory() {
		return this.territory;
	}
	public int getTerritoryArmyCount() {
		return this.territoryArmyCount;
	}
	public int getArmiesRemaining() {
		return this.armiesRemaining;
	}
}
