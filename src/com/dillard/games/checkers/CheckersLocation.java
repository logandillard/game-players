package com.dillard.games.checkers;

import java.io.Serializable;

public class CheckersLocation implements Serializable {
    private static final long serialVersionUID = 1L;

    private static CheckersLocation[][]
        locations = new CheckersLocation[CheckersBoard.NUM_ROWS][CheckersBoard.NUM_COLS];

    static {
        for (int r=0; r<8; r++) {
            for (int c=0; c<8; c++) {
                locations[r][c] = new CheckersLocation(r, c);
            }
        }
    }

	public final int row;
	public final int col;

	private CheckersLocation(int r, int c) {
		this.row = r;
		this.col = c;
	}

	public static CheckersLocation forLocation(int row, int col) {
	    return locations[row][col];
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
	    return 31 * row + col;
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
