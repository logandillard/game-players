package checkers;

import games.Move;

import java.util.ArrayList;
import java.util.List;

public class CheckersMove implements Move {
	
	private CheckersLocation from;
	private CheckersLocation to;
	// This needs to be a list because we can have multiple jumps!
	private List<CheckersLocation> jumpedPieces = null;

	public CheckersMove(CheckersLocation from, CheckersLocation to) {
		super();
		this.from = from;
		this.to = to;
	}
	
	public void addJumpedPiece(CheckersLocation jumpedLocation) {
		if (jumpedPieces == null) {
			jumpedPieces = new ArrayList<CheckersLocation>();
		} 
		jumpedPieces.add(jumpedLocation);
	}
	public void setJumpedPieces(List<CheckersLocation> jumped) {
		jumpedPieces = jumped;
	}
	
	public void setFrom(CheckersLocation from) {
		this.from = from;
	}
	public CheckersLocation getFrom() {
		return from;
	}
	public CheckersLocation getTo() {
		return to;
	}
	public boolean hasJumpedPieces() {
		return jumpedPieces != null && jumpedPieces.size() > 0;
	}
	public List<CheckersLocation> getJumpedPieces() {
		return jumpedPieces;
	}
	
	public String toString() {
		return from.toString() + " -> " + to.toString() + 
			(hasJumpedPieces() ? " jumping " + jumpedPieces.size() : "");
	}
	
	public boolean equals (Object o) {
		if (!(o instanceof CheckersMove)) return false;
		CheckersMove other = (CheckersMove)o;
		boolean toAndFrom = this.from.equals(other.from) &&
			   					this.to.equals(other.to) ;
		if (!toAndFrom) return false;
		
		if (jumpedPieces == null && other.jumpedPieces == null) return true;
		else if (jumpedPieces == null || other.jumpedPieces == null) return false;
		
		if (jumpedPieces.size() != other.jumpedPieces.size()) return false;
		
		for (int i=0; i< jumpedPieces.size(); i++) {
			if (jumpedPieces.get(i) != other.jumpedPieces.get(i)) return false;
		}
		return true;
		
	}
}
