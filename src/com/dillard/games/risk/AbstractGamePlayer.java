package com.dillard.games.risk;

public abstract class AbstractGamePlayer implements RiskPlayer {
	protected String name;

	public AbstractGamePlayer(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return name + " " + super.toString();
	}
}
