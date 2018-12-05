package com.dillard.games.checkers;

public class CheckersLocation {
	private static CheckersLocation[][]
        locations = new CheckersLocation[CheckersBoard.NUM_ROWS][CheckersBoard.NUM_COLS];

	private int row;
	private int col;

	private CheckersLocation(int r, int c) {
		this.row = r;
		this.col = c;
	}

	public static CheckersLocation forLocation(int row, int col) {
		if (!CheckersBoard.isWithinBounds(row) || !CheckersBoard.isWithinBounds(col)) {
			return null;
		}
		CheckersLocation l = locations[row][col];
		if (l == null) {
			l = new CheckersLocation(row, col);
			locations[row][col] = l;
		}
		return l;
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	@Override
    public String toString() {
		return row + "," + col;
	}

	@Override
    public boolean equals(Object o) {
		if (!(o instanceof CheckersLocation)) return false;
		CheckersLocation other = (CheckersLocation)o;
		return other.row == this.row && other.col == this.col;
	}
}
