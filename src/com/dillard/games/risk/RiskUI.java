package com.dillard.games.risk;

import java.util.List;

public interface RiskUI {

	void showGame(RiskGameState gameState);

	Move<RiskGame> getMove(List<Move<RiskGame>> moves);

	void showPlayerSummary(RiskGameState gameState, RiskPlayer player);
}
