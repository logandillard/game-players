package checkers.ui;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JPanel;

import checkers.CheckersLocation;
import checkers.PieceColor;

public class CheckersBoardSquare extends JPanel {
    private static final long serialVersionUID = 1L;
    private CheckersLocation myLocation;
	private boolean canHaveChecker = false;
	private CheckerPiece piece = null;

	public CheckersBoardSquare(CheckersLocation location, boolean canHaveChecker) {
		myLocation = location;
		this.canHaveChecker = canHaveChecker;
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		for (Component comp : this.getComponents()) {
			comp.paint(g);
		}
	}
	
	public void addChecker(PieceColor color, boolean isKing) {
		CheckerPiece piece = new CheckerPiece(color, isKing);
		this.piece = piece;
		add(piece);
		repaint();
	}
	
	public void removeChecker() {
		this.removeAll();
		this.repaint();
		this.piece = null;
	}
	
	public void toggleSelected() {
		if (hasChecker()) {
			piece.toggleSelected();
		}
		repaint();
	}
	
	public CheckersLocation getCheckersLocation() {
		return myLocation;
	}
	public boolean hasChecker() {
		return this.piece != null;
	}
	public boolean canHaveChecker() {
		return canHaveChecker;
	}
	
	public CheckerPiece getPiece() {
		return piece;
	}
	
	public String toString() {return myLocation.toString();}
}
