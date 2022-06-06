package com.dillard.games.risk;

public class TerritoryPair {
	public final Territory a;
	public final Territory b;

	public TerritoryPair(Territory a, Territory b) {
		this.a = a;
		this.b = b;
	}

	public String toString() {
		return a.toString() + " -> " + b.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
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
		TerritoryPair other = (TerritoryPair) obj;
		if (a != other.a)
			return false;
		if (b != other.b)
			return false;
		return true;
	}
}
