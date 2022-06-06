package com.dillard.games.risk;

public abstract class RiskMove implements Move<RiskGame> {
	private TurnPhase phase;

	public RiskMove(TurnPhase phase) {
		this.phase = phase;
	}

	public TurnPhase getPhase() {
		return phase;
	}
}
