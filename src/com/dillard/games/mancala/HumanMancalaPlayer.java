package com.dillard.games.mancala;

import java.util.List;

import com.dillard.games.HumanPlayer;
import com.dillard.games.IOUtilitiesInterface;

public class HumanMancalaPlayer extends HumanPlayer<MancalaMove, Mancala> {

	public HumanMancalaPlayer(int lowMove, int highMove, int offset, IOUtilitiesInterface iou) {
		super(lowMove, highMove, offset, iou);
	}

	@Override
	protected MancalaMove getMove(IOUtilitiesInterface iou2, String message, Mancala theGame, List<MancalaMove> moves) {
		return new MancalaMove(iou.getInt(message, lowMove, highMove));
	}

}
