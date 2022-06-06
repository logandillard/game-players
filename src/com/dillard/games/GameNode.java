package com.dillard.games;
import java.util.ArrayList;
import java.util.List;

public class GameNode<M extends Move, G extends Game<M, G>> implements Comparable<GameNode<M, G>> {
	private G game;
	private M move;
	private double score;
	private List<GameNode<M, G>> children;

	GameNode(G g, M m) {
		game = g.clone();
		move = m;
		children = null;
	}

	public void addChild(GameNode<M, G> child) {
		if (children == null) children = new ArrayList<GameNode<M, G>>();
		children.add(child);
	}

	public GameNode<M, G> getChildForMove(M move) {
	    if (children != null) {
    	    for (var child : children) {
    	        if (child.move.equals(move)) {
    	            return child;
    	        }
    	    }
	    }
	    return null;
	}

	public List<GameNode<M, G>> getChildren() {
		return children;
	}
	public void clearChildren() {
		children = null;
	}
	public G getGame() {
		return game;
	}
	public M getMove() {
	    return move;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}

	public int compareTo(GameNode<M, G> o) {
		if(!(o instanceof GameNode)) throw new ClassCastException();

		double scoreDiff = this.getScore() - o.getScore();
		if(scoreDiff > 0) {
			return 1;
		}
		else if(scoreDiff < 0) {
			return -1;
		}
		else return 0;
	}

	@Override
    public String toString() {
		return game.toString() + "Score: " + getScore();
	}
}
