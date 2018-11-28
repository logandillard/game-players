package com.dillard.games.risk;

public class MoveAdvance extends RiskMove implements Move<RiskGame> {
	private TerritoryState from;
	private TerritoryState to;
	private int armiesRemaining;

	public MoveAdvance(TerritoryState from, TerritoryState to, int armiesRemaining) {
		super(TurnPhase.ATTACK);
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
		return "AdvanceMove: " + from + " -> " + to;
	}
}
