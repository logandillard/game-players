package com.dillard.games.checkers;

import com.dillard.games.IOUtilitiesInterface;


public class HumanCheckersPlayer implements CheckersPlayer {
    private IOUtilitiesInterface iou;

	public HumanCheckersPlayer(IOUtilitiesInterface iou) {
		this.iou = iou;
	}

    @Override
    public CheckersMove move(CheckersGame game) {
        var moves = game.getMoves();
        var msg = "Choose your move";
        for (int i=0; i<moves.size(); i++) {
            msg += "\n" + i + ".\t" + moves.get(i);
        }

//      Set<CheckersLocation> fromLocations = new HashSet<CheckersLocation>();
//      for (Move m : moves) {
//          fromLocations.add( ((CheckersMove)m).getFrom() );
//      }
//
//      char initial = 'n';
//      Map<CheckersLocation, String> locationReplacements = new HashMap<CheckersLocation, String>();
//      Map<String, CheckersLocation> locationsForReplacements = new HashMap<String, CheckersLocation>();
//      for (CheckersLocation loc : fromLocations) {
//          locationReplacements.put(loc, "" + initial);
//          locationsForReplacements.put("" + initial, loc);
//          initial++;
//      }
//
//      String movesBoard = theGame.toString(locationReplacements);
//      System.out.println(movesBoard);

        int moveNum = iou.getInt(msg + "\n",0,moves.size()-1);
        return moves.get(moveNum);
    }

}
