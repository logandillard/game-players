package com.dillard.games;

public class GameTree {
	GameNode head;
	
	public GameTree(GameNode head) {
		this.head = head;
	}
	
	public GameNode addChild(GameNode parent, Game childGame) {
		GameNode child = new GameNode(childGame);
		parent.getChildren().add(child);
		return child;
	}

	public GameNode getHead() {
		return head;
	}

	public void setHead(GameNode head) {
		this.head = head;
	}
}
