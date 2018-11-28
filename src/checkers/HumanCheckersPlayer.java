package checkers;

import ioutilities.IOUtilitiesInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.Game;
import games.HumanPlayer;
import games.Move;

public class HumanCheckersPlayer extends HumanPlayer<Checkers> {

	public HumanCheckersPlayer(IOUtilitiesInterface iou) {
		super(iou);
	}
	
	@Override
	protected Move getMove(IOUtilitiesInterface iou, String message,
			Checkers theGame, List<Move> moves) {
		String msg = message;
		for (int i=0; i<moves.size(); i++) {
			msg += "\n" + i + ".\t" + moves.get(i);
		}
		
//		Set<CheckersLocation> fromLocations = new HashSet<CheckersLocation>();
//		for (Move m : moves) {
//			fromLocations.add( ((CheckersMove)m).getFrom() );
//		}
//		
//		char initial = 'n';
//		Map<CheckersLocation, String> locationReplacements = new HashMap<CheckersLocation, String>();
//		Map<String, CheckersLocation> locationsForReplacements = new HashMap<String, CheckersLocation>();
//		for (CheckersLocation loc : fromLocations) {
//			locationReplacements.put(loc, "" + initial);
//			locationsForReplacements.put("" + initial, loc);
//			initial++;
//		}
//		
//		String movesBoard = theGame.toString(locationReplacements); 
//		System.out.println(movesBoard);
		
		int moveNum = iou.getInt(msg + "\n",0,moves.size()-1);
		return moves.get(moveNum);
	}

}
