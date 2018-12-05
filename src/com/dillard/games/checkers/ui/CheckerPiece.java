package com.dillard.games.checkers.ui;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import com.dillard.games.checkers.PieceColor;

public class CheckerPiece extends JPanel {
    private static final long serialVersionUID = 1L;
    private Color c = Color.red;
	private int radius = 15;
	private boolean selected = false;
	private PieceColor pieceColor;
	private boolean isKing;


	public CheckerPiece(PieceColor color, boolean isKing) {
		pieceColor = color;
		this.isKing = isKing;

		if (color == PieceColor.WHITE) {
			c = Color.red;
		} else {
			c = Color.black;
		}
	}
	@Override
    public void paint(Graphics g) {
		fillCircle(radius, g);
	}
	private void fillCircle(int radius, Graphics g){
		g.setColor(c);
		int halfBoxSize = 25;
		g.fillOval(halfBoxSize - radius, halfBoxSize - radius, radius*2, radius*2);

		if (isKing) {
			int kingR = 5;
			g.setColor(Color.yellow);
			g.fillOval(halfBoxSize - kingR, halfBoxSize - kingR, kingR*2, kingR*2);
		}

		if (selected) {
			g.setColor(Color.white);
			g.drawOval(halfBoxSize - radius, halfBoxSize - radius, radius*2, radius*2);
		}
	}

	public boolean isSelected() {
		return selected;
	}
	public void toggleSelected() {
		selected = !selected;
	}
	public PieceColor getPieceColor() {
		return pieceColor;
	}
}
