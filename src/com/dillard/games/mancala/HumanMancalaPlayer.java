package com.dillard.games.mancala;

import java.util.List;

import com.dillard.games.HumanPlayer;
import com.dillard.games.IOUtilitiesInterface;
import com.dillard.games.Move;

public class HumanMancalaPlayer extends HumanPlayer<Mancala> {

	public HumanMancalaPlayer(int lowMove, int highMove, int offset, IOUtilitiesInterface iou) {
		super(lowMove, highMove, offset, iou);
	}

	@Override
	protected Move getMove(IOUtilitiesInterface iou2, String message, Mancala theGame, List<Move> moves) {
		return new MancalaMove(iou.getInt(message, lowMove, highMove));
	}

}
