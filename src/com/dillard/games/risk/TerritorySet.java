package com.dillard.games.risk;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public final class TerritorySet {
	private boolean[] values = new boolean[Territory.NUM_TERRITORIES];

	public void add(Territory t) {
		values[t.ordinal()] = true;
	}

	public boolean contains(Territory t) {
		return values[t.ordinal()];
	}

	public void addAll(Collection<Territory> territories) {
		for (Territory t : territories) {
			values[t.ordinal()] = true;
		}
	}

	public List<Territory> values() {
		List<Territory> l = new ArrayList<>(values.length); // this avoids resizing
		for (int i=0; i<values.length; i++) {
			if (values[i]) {
				l.add(Territory.VALUES[i]);
			}
		}
		return l;
	}

	public List<Territory> values(TerritorySet excludeSet) {
		List<Territory> l = new ArrayList<>(values.length); // this avoids resizing
		for (int i=0; i<values.length; i++) {
			if (values[i]) {
				Territory t = Territory.VALUES[i];
				if (!excludeSet.contains(t)) {
					l.add(t);
				}
			}
		}
		return l;
	}
}
