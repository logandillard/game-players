package com.dillard.games.risk;

import java.util.Arrays;

public final class TerritoryMapToInt {
	public static final int DEFAULT_VALUE = -Integer.MAX_VALUE;

	private int[] values = new int[Territory.VALUES.length];

	public TerritoryMapToInt() {
		Arrays.fill(values, DEFAULT_VALUE);
	}

	public void put(Territory t, int i) {
		values[t.ordinal()] = i;
	}

	public int get(Territory t) {
		return values[t.ordinal()];
	}

	public void remove(Territory t) {
		values[t.ordinal()] = DEFAULT_VALUE;
	}

	public void clear() {
		for (int i=0; i<values.length; i++) {
			values[i] = DEFAULT_VALUE;
		}
	}
}
