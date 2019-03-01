package com.dillard.games;

public class GameTree<M extends Move, G extends Game<M, G>> {
	GameNode<M, G> head;

	public GameTree(GameNode<M, G> head) {
		this.head = head;
	}

	public GameNode<M, G> addChild(GameNode<M, G> parent, G childGame) {
		GameNode<M, G> child = new GameNode<M, G>(childGame);
		parent.getChildren().add(child);
		return child;
	}

	public GameNode<M, G> getHead() {
		return head;
	}

	public void setHead(GameNode<M, G> head) {
		this.head = head;
	}
}
