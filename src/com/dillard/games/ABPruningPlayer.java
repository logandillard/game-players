package com.dillard.games;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ABPruningPlayer<M extends Move, G extends Game<M, G>> implements GamePlayer<M, G> {
	private GameNode<M, G> root = null;
	private boolean isPlayer1;

	int childrenFound=0, childrenMissing=0;

	private int turnDepthLimit;
	private int numLeafNodes = 0;
	private Random random;

	public ABPruningPlayer(int turnDepthLim, Random rand) {
		if (turnDepthLim < 1) {
			throw new RuntimeException("Turn depth limit must be >= 1");
		}

		this.turnDepthLimit = turnDepthLim;
		this.random = rand;
	}

	public M move(G theGame) {
	    numLeafNodes = 0;

	    // TODO should re-use game trees across calls
	    if (root == null) {
	        root = new GameNode<M, G>(theGame, null);
	    }
		isPlayer1 = theGame.isPlayer1Turn();

		// For each move in head, score the results of the moves
		maxValue(root, 0, -Double.MAX_VALUE, Double.MAX_VALUE);

		// Return the highest valued move
		List<GameNode<M, G>> children = root.getChildren();
		if (children == null) {
			throw new RuntimeException("Game tree is empty");
		}

		// Find the high score
		double highScore = children.stream()
		        .max(Comparator.comparing(GameNode::getScore))
		        .orElseThrow()
		        .getScore();
//		double highScore = -Double.MAX_VALUE;
//		for (GameNode<M, G> child : children) {
//            if (child.getScore() > highScore) {
//                highScore = child.getScore();
//            }
//		}

		// Make a list of all moves that result in the high score
		List<M> highScoreMoveList = children.stream()
		        .filter(child -> child.getScore() == highScore)
		        .map(child -> child.getMove())
		        .collect(Collectors.toList());
//		List<M> highScoreMoveList = new ArrayList<M>();
//		for (GameNode<M, G> child : children) {
//			if (child.getScore() == highScore) {
//				highScoreMoveList.add(child.getMove());
//			}
//		}

		// Pick one move at random from that list
		int highScoreMoveIndex = random.nextInt(highScoreMoveList.size());

//		System.out.println(childrenFound / (double) (childrenFound + childrenMissing));

		return highScoreMoveList.get(highScoreMoveIndex);
	}

	private double maxValue(GameNode<M, G> gameNode, int turnDepth, double alpha, double beta) {
		return minMaxValue(true, gameNode, turnDepth, alpha, beta);
	}

	private double minValue(GameNode<M, G> gameNode, int turnDepth, double alpha, double beta) {
	    return minMaxValue(false, gameNode, turnDepth, alpha, beta);
	}

	private double minMaxValue(boolean maximize, GameNode<M, G> gameNode, int turnDepth, double alpha, double beta) {
		G theGame = gameNode.getGame();
		if (cutoff(theGame, turnDepth)) {
		    numLeafNodes++;
			return evaluate(theGame);
		}

		double highestOrLowest = maximize
		        ? -Double.MAX_VALUE
		        : Double.MAX_VALUE;

		// For each move and resulting state,
		// beta = min(beta, maxValue(theGame, turnDepth+1, alpha, beta))
		// if(alpha >= beta) return alpha;
		boolean hadChildren = gameNode.getChildren() != null;
		for (M move: theGame.getMoves()) {

		    GameNode<M, G> childNode = gameNode.getChildForMove(move);
		    if (childNode == null) { // had not already explored this node
		        childrenMissing++;
//		        if (hadChildren) {
//                    System.out.println("Move was not found in children!");
//                    System.out.println(move);
//                    var moves = gameNode.getChildren().stream()
//                        .map(child -> child.getMove())
//                        .collect(Collectors.toList());
//                    System.out.println(moves);
//		        }

    			// Perform each move
    			G tempGame = theGame.clone();
    			tempGame.move(move);
    			childNode = new GameNode<M, G>(tempGame, move);
    			gameNode.addChild(childNode);
		    } else {
		        childrenFound++;
		    }

			// AB-pruning logic
			// Get either the min value or the max value, depending whose turn is next
			// (does not strictly alternate in case it is my turn twice in a row)
			double tempVal = (isPlayer1 == childNode.getGame().isPlayer1Turn() ?
					    maxValue(childNode, turnDepth+1, alpha, beta) :
						minValue(childNode, turnDepth+1, alpha, beta));
			childNode.setScore(tempVal);
			if (maximize) {
	            alpha = Math.max(alpha, tempVal);
	            highestOrLowest = Math.max(highestOrLowest, tempVal);

	            // Pruning condition
	            if (alpha >= beta) return highestOrLowest;
			} else {
    			beta = Math.min(beta, tempVal);
    			highestOrLowest = Math.min(highestOrLowest, tempVal);

    			// Pruning condition
    			if (beta < alpha) return highestOrLowest;
			}
		}

		// Return the highest/lowest value just found in this method, not the alpha input value
		return highestOrLowest;
	}

    public void advanceToMove(M move) {
        if (root != null) {
            var oldRoot = root;
            root = root.getChildForMove(move);
            if (root == null && oldRoot.getChildren() != null) {
                System.out.println("Move was not found in children!");
                System.out.println(move);
                var moves = oldRoot.getChildren().stream()
                    .map(child -> child.getMove())
                    .collect(Collectors.toList());
                System.out.println(moves);
            }
        }
    }

	private boolean cutoff(Game<M, G> theGame, int turnDepth) {
		if (theGame.isTerminated() || turnDepth == turnDepthLimit) {
			return true;
		}
		if (turnDepth > turnDepthLimit) {
		    throw new RuntimeException("AB pruning player went past its limit!");
		}
		return false;
	}

	private double evaluate(G theGame) {
		return theGame.evaluate(isPlayer1);
	}

	public boolean isPlayer1() {
		return isPlayer1;
	}
}
