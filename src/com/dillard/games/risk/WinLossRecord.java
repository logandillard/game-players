package com.dillard.games.risk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WinLossRecord {
	private Collection<String> playerNames;
	private Map<String, Integer> winCountByPlayer = new HashMap<>();
	private int totalGames = 0;

	public WinLossRecord(Collection<String> players) {
		this.playerNames = players;
		for (String name : players) {
			winCountByPlayer.put(name, 0);
		}
	}
	
	public void addWin(String playerName) {
		totalGames++;
		if (!winCountByPlayer.containsKey(playerName)) {
			playerNames.add(playerName);
			winCountByPlayer.put(playerName, 1);
		} else {
			winCountByPlayer.put(playerName, winCountByPlayer.get(playerName) + 1);
		}
	}

	public int getWinCount(String playerName) {
		return winCountByPlayer.get(playerName);
	}

	public int getTotalGames() {
		return totalGames;
	}

	public double getWinRatio(String playerName) {
		return getWinCount(playerName) / (double) totalGames;
	}

	public Collection<String> getPlayerNames() {
		return playerNames;
	}
}
