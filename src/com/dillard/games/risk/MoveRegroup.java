package com.dillard.games.risk;

public class MoveRegroup extends RiskMove implements Move<RiskGame> {
	private TerritoryState from;
	private TerritoryState to;
	private int armiesRemaining;

	public MoveRegroup(TerritoryState from, TerritoryState to, int armiesRemaining) {
		super(TurnPhase.REGROUP);
		this.from = from;
		this.to = to;
		this.armiesRemaining = armiesRemaining;
	}

	public TerritoryState getFrom() {
		return from;
	}

	public TerritoryState getTo() {
		return to;
	}

	public int getArmiesRemaining() {
		return armiesRemaining;
	}

	public void execute(RiskGame game) {
		game.applyMove(this);
	}

	public String toString() {
		return "RegroupMove: " + from + " -> " + to;
	}
}
