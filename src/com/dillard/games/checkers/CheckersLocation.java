package com.dillard.games.checkers;

public class CheckersLocation {
	private static CheckersLocation[][]
        locations = new CheckersLocation[CheckersBoard.NUM_ROWS][CheckersBoard.NUM_COLS];

	public final int row;
	public final int col;

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + col;
        result = prime * result + row;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CheckersLocation other = (CheckersLocation) obj;
        if (col != other.col)
            return false;
        if (row != other.row)
            return false;
        return true;
    }
}
