package com.dillard.games.risk;

import java.util.List;

public interface RiskPlayer extends GamePlayer<RiskGame> {
	Move<RiskGame> getMove(RiskGameState gameState, List<Move<RiskGame>> moves);
}
