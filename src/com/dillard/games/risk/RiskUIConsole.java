package com.dillard.games.risk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RiskUIConsole implements RiskUI {

	@Override
	public void showGame(RiskGameState state) {
		System.out.println(showGameBasic(state));
	}

	public String showGameBasic(RiskGameState state) {
		StringBuilder sb = new StringBuilder("\n");
		for (Continent continent : Continent.values()) {
			Set<String> owners = 
			    continent.getTerritoryList().stream()
					.map(t -> state.getTerritoryState(t))
					.map(ts -> ts.getOwner().getName())
					.collect(Collectors.toSet());
			sb.append(continent + "  " + owners + "\n");
			for (Territory t : continent.getTerritoryList()) {
				sb.append(state.getTerritoryState(t) + "\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public void showGameMap(RiskGameState s) {
		System.out.println();
		pt(Territory.ALASKA, s);  
		pt(Territory.NORTHWEST_TERRITORY, s);
		pt(Territory.GREENLAND, s);
		// TODO
		System.out.println();
		
	}
	
	/** print territory */
	private void pt(Territory t, RiskGameState s) {
		TerritoryState ts = s.getTerritoryState(t);
		String name = ts.getOwner().getName();
		int armyCount = ts.getArmyCount();
		System.out.print(pad(25, "[" + name + " (" + armyCount + ")] "));
	}

	private String pad(int i, String s) {
		StringBuilder sb = new StringBuilder(s);
		while (sb.length() < i) {
			sb.append(" ");
		}
		return s;
	}

	@Override
	public Move<RiskGame> getMove(List<Move<RiskGame>> moves) {
		for (int i=0; i<moves.size(); i++) {
			System.out.println(i + "   " + moves.get(i));
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		do {
			System.out.println("Enter move number");
			try {
				String input = reader.readLine();
				if (input.isEmpty()) continue;

				int choice = Integer.parseInt(input);
				if (choice < 0 || choice >= moves.size()) {
					continue;
				}

				return moves.get(choice);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} while (true);
	}

	@Override
	public void showPlayerSummary(RiskGameState gameState, RiskPlayer player) {
		System.out.println(player.getName() + 
				" (" + gameState.getArmyAllowance(player) + 
				") cards: " + gameState.getCardCount(player));
	}

}
