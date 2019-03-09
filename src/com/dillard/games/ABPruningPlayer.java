package com.dillard.games;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ABPruningPlayer<M extends Move, G extends Game<M, G>> implements GamePlayer<M, G> {
	private GameTree<M, G> gameTree;
	private boolean isPlayer1;

	private int turnDepthLimit;
	private int numLeafNodes = 0;

	public ABPruningPlayer(int turnDepthLim) {
		if (turnDepthLim < 1) {
			throw new RuntimeException("Turn depth limit must be >= 1");
		}

		turnDepthLimit = turnDepthLim;
		gameTree = null;
	}

	public M move(G theGame) {
	    numLeafNodes = 0;

	    // TODO should re-use game trees across calls
		gameTree = new GameTree<M, G>(new GameNode<M, G>(theGame));
		isPlayer1 = theGame.isPlayer1Turn();
		GameNode<M, G> head = gameTree.getHead();

		// For each move in head, score the results of the moves
		maxValue(head, 0, -Double.MAX_VALUE, Double.MAX_VALUE);

		// Return the highest valued move
		List<GameNode<M, G>> chil = gameTree.getHead().getChildren();
		if(chil == null) {
			throw new RuntimeException("Game tree is empty");
		}
		Iterator<GameNode<M, G>> itr = chil.iterator();
		List<M> moves = head.getGame().getMoves();
		int i=0;
		GameNode<M, G> node;
		double highScore = -Double.MAX_VALUE;

		// Find the high score
		while(itr.hasNext()) {
			node = itr.next();
			if(node.getScore() > highScore) {
				highScore = node.getScore();
			}
			i++;
		}

		// Make a list of all moves that result in the high score
		itr = chil.iterator();
		i=0;
		List<M> highScoreMoveList = new ArrayList<M>();

		while(itr.hasNext()) {
			node = itr.next();
			if(node.getScore() == highScore) {
				highScoreMoveList.add(moves.get(i));
			}
			i++;
		}

//		System.out.println(numLeafNodes);

		// Pick one move at random from that list
		int highScoreMoveIndex = (int) (Math.random() * highScoreMoveList.size());
		return highScoreMoveList.get(highScoreMoveIndex);
	}

	private double maxValue(GameNode<M, G> gameNode, int turnDepth, double alpha, double beta) {
		G theGame = gameNode.getGame();
		if (cutoff(theGame, turnDepth)) {
		    numLeafNodes++;
			return evaluate(theGame);
		}

		List<M> moves = theGame.getMoves();
		G tempGame;
		GameNode<M, G> addedNode;
		double tempVal;
		double highestFound = -Double.MAX_VALUE;

		// For each move and resulting state,
		// alpha = max(alpha, minValue(theGame, turnDepth+1, alpha, beta))
		// if(alpha >= beta) return beta;
		for (M m: moves) {
			// Perform each move
			tempGame = theGame.clone();
			try{
				tempGame.move(m);
			} catch(InvalidMoveException exc) {
				throw new IllegalArgumentException();
			}
			addedNode = new GameNode<M, G>(tempGame);
			if (turnDepth == 0) gameNode.addChild(addedNode); // TODO

			// AB-pruning logic
			// Get either the min value or the max value, depending whose turn is next
			// (does not strictly alternate in case it is my turn twice in a row
			// for some reason in the game)
			tempVal = (isPlayer1 == addedNode.getGame().isPlayer1Turn() ?
					maxValue(addedNode, turnDepth+1, alpha, beta) :
						minValue(addedNode, turnDepth+1, alpha, beta));
			addedNode.setScore(tempVal);
			alpha = Math.max(alpha, tempVal);
			highestFound = Math.max(highestFound, tempVal);

			// Pruning condition
			if(alpha >= beta) return highestFound;
		}

//		if (turnDepth > 0) {
//			// delete children to save memory
//			gameNode.clearChildren();
//		}

		// Return the highest value just found in this method, not the alpha input value
		return highestFound;
	}

	private double minValue(GameNode<M, G> gameNode, int turnDepth, double alpha, double beta) {
		G theGame = gameNode.getGame();
		if(cutoff(theGame, turnDepth)) {
			return evaluate(theGame);
		}

		List<M> moves = theGame.getMoves();
		G tempGame;
		GameNode<M, G> addedNode;
		double tempVal;
		double lowestFound = Double.MAX_VALUE;

		// For each move and resulting state,
			// beta = min(beta, maxValue(theGame, turnDepth+1, alpha, beta))
			// if(alpha >= beta) return alpha;
		for (M m: moves) {
			// Perform each move
			tempGame = theGame.clone();
			try{
				tempGame.move(m);
			} catch(InvalidMoveException exc) {
				throw new IllegalArgumentException();
			}
			addedNode = new GameNode<M, G>(tempGame);
			if (turnDepth == 0) gameNode.addChild(addedNode); // TODO

			// AB-pruning logic
			// Get either the min value or the max value, depending whose turn is next
			// (does not strictly alternate in case it is my turn twice in a row
			// for some reason in the game)
			tempVal = (isPlayer1 == addedNode.getGame().isPlayer1Turn() ?
					maxValue(addedNode, turnDepth+1, alpha, beta) :
						minValue(addedNode, turnDepth+1, alpha, beta));
			addedNode.setScore(tempVal);
			beta = Math.min(beta, tempVal);
			lowestFound = Math.min(lowestFound, tempVal);

			// Pruning condition
			if(beta < alpha) return lowestFound;
		}

//		if (turnDepth > 0) {
//			// delete children to save memory
//			gameNode.clearChildren();
//		}

		// Return the lowest value just found in this method, not the alpha input value
		return lowestFound;
	}


	protected boolean cutoff(Game<M, G> theGame, int turnDepth) {
		if(theGame.isTerminated() || turnDepth == turnDepthLimit) {
			return true;
		}
		if(turnDepth > turnDepthLimit) throw new RuntimeException("AB pruning player went past its limit!");
		return false;
	}

	protected double evaluate(G theGame) {
		return theGame.evaluate(isPlayer1);
	}
	public boolean isPlayer1() {
		return isPlayer1;
	}
}
