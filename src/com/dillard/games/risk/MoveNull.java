package com.dillard.games.risk;

public class MoveNull extends RiskMove implements Move<RiskGame> {
	public MoveNull() {
		super(null);
	}

	public void execute(RiskGame game) {
		game.applyMove(this);
	}

	public String toString() {
		return "End phase";
	}
}
