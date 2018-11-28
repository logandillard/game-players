package games;

import java.util.List;

public abstract class NoLookNNPlayer<G extends Game> implements GamePlayer<G> {

	@Override
	public Move move(G theGame) {
		
		// Setup
		List<Move> moves = theGame.getMoves();
		boolean NNisPlayer1 = theGame.isPlayer1Turn();
		Game[] possibleStates = new Game[moves.size()];
		G tempGame;
		double tempVal = Double.MAX_VALUE;
		Move m;
		double highestFound = -Double.MAX_VALUE;
		int highestFoundIndex = -1;
		
		
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// THIS USES NO LOOKAHEAD AT ALL
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		
		
		// Generate all moves
		for(int i=0; i<moves.size(); i++) {
			m = moves.get(i);

			// Perform each move
			tempGame = (G)theGame.clone();
			try{
				tempGame.move(m);
			} catch(InvalidMoveException exc) {
				throw new IllegalArgumentException(exc);
			}
			possibleStates[i] = tempGame;
			
			// Calculate the neuralNet's predicted value for this move option.
			tempVal = getNNScoreForGamestate(tempGame, NNisPlayer1);
			
			// record the lowest value found so far (lowest chances of winning for the other player)
			if(tempVal > highestFound) {
				highestFound = tempVal;
				highestFoundIndex = i;
			}
		}
		
		if(highestFoundIndex < 0) {
			throw new ModelBlewUpException();
		}

		// return the move with the highest predicted value
		return moves.get(highestFoundIndex);
	}
	
	protected abstract double getNNScoreForGamestate(G game, boolean isPlayer1) ;
	
	protected abstract double[] boardToNNInputs(G game, boolean NNisPlayer1) ;

}
