package com.dillard.games.risk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum Continent {
	NORTH_AMERICA(5), SOUTH_AMERICA(2), EUROPE(5), ASIA(7), AFRICA(3), AUSTRALIA(2);
	
	private int reward;
	private Set<Territory> territorySet = new HashSet<>();
	private List<Territory> territoryList = new ArrayList<>();
	
	private Continent(int reward) {
		this.reward = reward;
	}

	public int getReward() {
		return reward;
	}

	void addTerritory(Territory t) {
		territorySet.add(t);
		territoryList = new ArrayList<>(territorySet);
	}
	
	public List<Territory> getTerritoryList() {
		return territoryList;
	}

	public boolean contains(Territory t) {
		return territorySet.contains(t);
	}
}
