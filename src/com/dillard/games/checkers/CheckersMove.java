package com.dillard.games.checkers;

import java.io.Serializable;

import com.dillard.games.Move;
import com.dillard.games.checkers.MCTS.MCTSMove;

public class CheckersMove implements Move, MCTSMove, Serializable {
    private static final long serialVersionUID = 1L;

    public final CheckersLocation from;
	public final CheckersLocation to;
	public final CheckersLocation jumpedLocation;

	public CheckersMove(CheckersLocation from, CheckersLocation to) {
		this.from = from;
		this.to = to;
		this.jumpedLocation = null;
	}
    public CheckersMove(CheckersLocation from, CheckersLocation to, CheckersLocation jumpedLocation) {
        this.from = from;
        this.to = to;
        this.jumpedLocation = jumpedLocation;
    }

	@Override
    public String toString() {
		return from.toString() + " -> " + to.toString() +
			(jumpedLocation != null ? " jumping " + jumpedLocation.toString() : "");
	}

	@Override
    public int hashCode() {
        int result = 31 + ((from == null) ? 0 : from.hashCode());
        result = 31 * result + ((jumpedLocation == null) ? 0 : jumpedLocation.hashCode());
        result = 31 * result + ((to == null) ? 0 : to.hashCode());
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
        CheckersMove other = (CheckersMove) obj;
        if (from == null) {
            if (other.from != null)
                return false;
        } else if (!from.equals(other.from))
            return false;
        if (jumpedLocation == null) {
            if (other.jumpedLocation != null)
                return false;
        } else if (!jumpedLocation.equals(other.jumpedLocation))
            return false;
        if (to == null) {
            if (other.to != null)
                return false;
        } else if (!to.equals(other.to))
            return false;
        return true;
    }
}
