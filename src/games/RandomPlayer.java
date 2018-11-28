package games;

import java.util.List;

public class RandomPlayer implements GamePlayer {

	public Move move(Game theGame) {
		List<Move> moves = theGame.getMoves();
		return moves.get((int)(Math.random() * moves.size()));
	}

}
