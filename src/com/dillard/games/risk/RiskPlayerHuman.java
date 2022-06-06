package com.dillard.games.risk;

import java.util.List;

public class RiskPlayerHuman extends AbstractGamePlayer implements RiskPlayer {
	private RiskUI ui;

	public RiskPlayerHuman(String name, RiskUI ui) {
		super(name);
		this.ui = ui;
	}

	public Move<RiskGame> getMove(
			RiskGameState gameState,
			List<Move<RiskGame>> moves) {
		ui.showGame(gameState);
		ui.showPlayerSummary(gameState, this);
		return ui.getMove(moves);
	}
}
