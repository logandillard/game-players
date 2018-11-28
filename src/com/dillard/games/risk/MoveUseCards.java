package com.dillard.games.risk;

public class MoveUseCards extends RiskMove implements Move<RiskGame> {
	public final int currentReward;
	public final int numCards;

	public MoveUseCards(int currentReward, int numCards) {
		super(TurnPhase.USE_CARDS);
		this.currentReward = currentReward;
		this.numCards = numCards;
	}

	public void execute(RiskGame game) {
		game.applyMove(this);
	}

	@Override
	public String toString() {
		return "MoveUseCards [currentReward=" + currentReward + ", numCards="
				+ numCards + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + currentReward;
		result = prime * result + numCards;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MoveUseCards other = (MoveUseCards) obj;
		if (currentReward != other.currentReward)
			return false;
		if (numCards != other.numCards)
			return false;
		return true;
	}
	
}
