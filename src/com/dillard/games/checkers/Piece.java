package com.dillard.games.checkers;

import java.io.Serializable;

public class Piece implements Serializable {
    private static final long serialVersionUID = 1L;

	public final PieceColor color;
	public final boolean isKing;

	private Piece(PieceColor color, boolean isKing) {
		this.color = color;
		this.isKing = isKing;
	}

	private static Piece[] kings = {new Piece(PieceColor.WHITE, true), new Piece(PieceColor.BLACK, true)};
	private static Piece[] men = {new Piece(PieceColor.WHITE, false), new Piece(PieceColor.BLACK, false)};

	public static Piece forValue(PieceColor color, boolean isKing) {
		if (isKing) {
			return forValue(color, kings);
		} else {
			return forValue(color, men);
		}
	}

	private static Piece forValue(PieceColor color, Piece[] pieces) {
		if (color == PieceColor.WHITE) return pieces[0];
		else return pieces [1];
	}

	@Override
    public Piece clone() {
		return new Piece(color, isKing);
	}

	public PieceColor getColor() {
		return color;
	}

	public boolean isKing() {
		return isKing;
	}

	@Override
    public String toString() {
		return color.toString() + (isKing ? " K" : "");
	}
}
