package com.dillard.games.risk;

public class TerritoryState {
	private final Territory territory;
	private RiskPlayer owner;
	private int armyCount;

	public TerritoryState(Territory territory, RiskPlayer owner, int armyCount) {
		this.territory = territory;
		this.owner = owner;
		this.armyCount = armyCount;
	}

	public TerritoryState(Territory territory) {
		this.territory = territory;
		this.owner = null;
		this.armyCount = 0;
	}

	public Territory getTerritory() {
		return territory;
	}

	public RiskPlayer getOwner() {
		return owner;
	}

	public void setOwner(RiskPlayer owner) {
		this.owner = owner;
	}

	public int getArmyCount() {
		return armyCount;
	}

	public void setArmyCount(int armyCount) {
		this.armyCount = armyCount;
	}

	public void addArmies(int i) {
		setArmyCount(getArmyCount() + i);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + armyCount;
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result
				+ ((territory == null) ? 0 : territory.hashCode());
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
		TerritoryState other = (TerritoryState) obj;
		if (armyCount != other.armyCount)
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		if (territory != other.territory)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return territory + ", " + owner.getName() + " (" + armyCount + ")";
	}
}
