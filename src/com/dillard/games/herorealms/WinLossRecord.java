package com.dillard.games.herorealms;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class WinLossRecord {
	private Collection<String> playerNames = new HashSet<>();
	private Map<String, Integer> winCountByPlayer = new HashMap<>();
	private int totalGames = 0;

	public WinLossRecord() {
	}

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
		return winCountByPlayer.getOrDefault(playerName, 0);
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
