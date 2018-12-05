package com.dillard.games;
import java.util.*;

public class GameNode<G extends Game> implements Comparable {
	private G game;
	private double score;
	private ArrayList<GameNode<G>> children;
	
	
	//Constructor
	GameNode(G g) {
		game = (G) g.clone();
		children = null;
	}
	
	public void addChild(GameNode<G> child) {
		if(children == null) children = new ArrayList<GameNode<G>>();
		children.add(child);
	}
	
	public ArrayList<GameNode<G>> getChildren() {
		return children;
	}
//	public void setChildren(ArrayList<GameNode> children) {
//		this.children = children;
//	}
	public void clearChildren() {
		children = null;
	}
	public G getGame() {
		return game;
	}
//	public void setGame(Game game) {
//		this.game = game;
//	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	
	public int compareTo(Object o) {
		if(!(o instanceof GameNode)) throw new ClassCastException();
		
		double scoreDiff = this.getScore() - ((GameNode)o).getScore();
		if(scoreDiff > 0) {
			return 1;
		}
		else if(scoreDiff < 0) {
			return -1;
		}
		else return 0;
	}
	
	public String toString() {
		return game.toString() + "Score: " + getScore();
	}
}
