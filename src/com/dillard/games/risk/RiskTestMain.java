package com.dillard.games.risk;

public class RiskTestMain {
	public static void main(String[] args) {
		System.out.println("STARTING");
		Territory.ensureNoAdjacencyBugs();
		System.out.println("DONE");
	}
}
