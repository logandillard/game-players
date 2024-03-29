package com.dillard.games;

import java.util.List;

public abstract class HumanPlayer<M extends Move, G extends Game<M, G>> implements GamePlayer<M, G> {
	protected int lowMove, highMove, offset;
	protected IOUtilitiesInterface iou;

	public HumanPlayer(IOUtilitiesInterface iou) {
		this.iou = iou;
	}
	public HumanPlayer(int lowMove, int highMove, int offset, IOUtilitiesInterface iou) {
		this.iou = iou;
		this.lowMove = lowMove;
		this.highMove = highMove;
		this.offset = offset;
	}

	public M move(G theGame, boolean printBoardAndMessage) {
		String message = "";
		List<M> moves = theGame.getMoves();
		if(printBoardAndMessage) {
			System.out.println("\n\n" + theGame.toString());
			message = "\nYour move: ";
		}
		return getMove(iou, message, theGame, moves);
	}

	protected abstract M getMove(IOUtilitiesInterface iou2, String message, G theGame, List<M> moves) ;

	public M move(G theGame) {
		return move(theGame, true);
	}

}
