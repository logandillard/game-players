package com.dillard.games.risk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class TerritoryMap<T> {
	private Object[] values;
	
	public TerritoryMap() {
		values = new Object[Territory.VALUES.length];
	}

	public void put(Territory t, T i) {
		values[t.ordinal()] = i;
	}

	@SuppressWarnings("unchecked")
	public T get(Territory t) {
		return (T) values[t.ordinal()];
	}

	@SuppressWarnings("unchecked")
	public Collection<T> values() {
		List<T> l = new ArrayList<>(values.length);
		for (int i=0; i<values.length; i++) {
			if (values[i] != null) {
				l.add((T)values[i]);
			}
		}
		return l;
	}

	public void clear() {
		for (int i=0; i<values.length; i++) {
			values[i] = null;
		}
	}
}
