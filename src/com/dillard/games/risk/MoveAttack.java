package com.dillard.games.risk;

public class MoveAttack extends RiskMove implements Move<RiskGame> {
	private TerritoryState from;
	private TerritoryState to;

	public MoveAttack(TerritoryState from, TerritoryState to) {
		super(TurnPhase.ATTACK);
		this.from = from;
		this.to = to;
	}

	public TerritoryState getFrom() {
		return from;
	}

	public TerritoryState getTo() {
		return to;
	}

	public void execute(RiskGame game) {
		game.applyMove(this);
	}
	
	public String toString() {
		return "AttackMove: " + from.getTerritory() + " (" + from.getArmyCount() + ") -> " + to;
	}
}
