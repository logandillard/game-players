package games;
/**
 * @author Logan Dillard
 */
import java.util.List;

import ioutilities.*;

public abstract class HumanPlayer<G extends Game> implements GamePlayer<G> {
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
	
	public Move move(G theGame, boolean printBoardAndMessage) {
		String message = "";
		List<Move> moves = theGame.getMoves();
		if(printBoardAndMessage) {
			System.out.println("\n\n" + theGame.toString());
			message = "\nYour move: ";
		}
		return getMove(iou, message, theGame, moves);
	}
	
	protected abstract Move getMove(IOUtilitiesInterface iou2, String message, G theGame, List<Move> moves) ;

	public Move move(G theGame) {
		return move(theGame, true);
	}

}
