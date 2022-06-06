package com.dillard.games;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MinimaxPlayer<M extends Move, G extends Game<M, G>> implements GamePlayer<M, G> {
	GameTree<M, G> gameTree;
	boolean isPlayer1;
	boolean minimaxValuesAlreadyCreated = false;

	public M move(G game) {

		if (!(game instanceof SuccessorAwareGame)) {
			throw new RuntimeException("Minimax player can only play SuccessorAwareGames");
		}
		SuccessorAwareGame theGame = (SuccessorAwareGame) game;

		GameNode<M, G> head;
		isPlayer1 = theGame.isPlayer1Turn();

		if(gameTree == null) {
			gameTree = new GameTree<M, G>(new GameNode<M, G>((G) theGame, null));
		}
		else {
			// Find the correct branch of the existing game tree and reuse it
			List<GameNode<M, G>> children;
			boolean movedHead;
			do {
				movedHead = false;
				children = gameTree.getHead().getChildren();

				for(int i=0; i<children.size(); i++) {
					if(theGame.isSuccessor(children.get(i).getGame())) {
						gameTree.setHead(children.get(i));
						movedHead = true;
						minimaxValuesAlreadyCreated = true;
						break;
					}
				}
			}while(!(gameTree.getHead().getGame().equals(theGame)) && movedHead);
		}

		head = gameTree.getHead();

		// Set the minimax value for each move
		if(!minimaxValuesAlreadyCreated) setMinimaxValue(head);

		// return the highest valued move
		Iterator<GameNode<M, G>> itr = gameTree.getHead().getChildren().iterator();
		List<M> moves = head.getGame().getMoves();
		int i=0;
		GameNode<M, G> node;
		double highScore = -Double.MAX_VALUE;

		while(itr.hasNext()) {
			node = itr.next();
			if(node.getScore() > highScore) {
				highScore = node.getScore();
			}
			i++;
		}

		// Make a list of all moves that result in the high score
		itr = gameTree.getHead().getChildren().iterator();
		i=0;
		List<M> highScoreMoveList = new ArrayList<M>();

		while(itr.hasNext()) {
			node = itr.next();
			if(node.getScore() == highScore) {
				highScoreMoveList.add(moves.get(i));
			}
			i++;
		}

		// Pick one move at random from that list
		int highScoreMoveIndex = (int) (Math.random() * highScoreMoveList.size());
		return highScoreMoveList.get(highScoreMoveIndex);
	}



	private void setMinimaxValue(GameNode<M, G> gameNode) {
		G theGame = gameNode.getGame();
		// If terminated, then return score
		if(theGame.isTerminated()) {
			gameNode.setScore(theGame.getFinalScore(isPlayer1));
			return;
		}

		List<M> moves = theGame.getMoves();
		G tempGame;
		GameNode<M, G> addedNode;


		// Calculate the minimax value of all available moves
		for(M m: moves) {
			tempGame = theGame.clone();
			try{
				tempGame.move(m);
			} catch(InvalidMoveException exc) {
				throw new IllegalArgumentException();
				//continue;
			}
			addedNode = new GameNode<M, G>(tempGame, m);
			gameNode.addChild(addedNode);
//			addedNode.setScore(minimaxValue(addedNode));
			setMinimaxValue(addedNode);
		}

		// If my turn, return highest value of all of next moves
		if(theGame.isPlayer1Turn() == isPlayer1) {
			gameNode.setScore(Collections.max(gameNode.getChildren()).getScore());
			return;
		}
		// else (not my turn), return lowest value of all of next moves
		else {
			gameNode.setScore(Collections.min(gameNode.getChildren()).getScore());
			return;
		}


//		Game theGame = currentNode.getGame();
//		Game tempGame;
//
//		// For each move
//		for(int i=0; i<moves.length; i++) {
//			// the value of the move = minimaxValue(the game after move applied)
//			tempGame = theGame.clone();
//
//			try{
//				tempGame.move(moves[i]);
//			} catch(InvalidMoveException exc) {
//				throw new IllegalArgumentException();
//				//continue;
//			}
//			GameNode addedNode = gameTree.addChild(currentNode, tempGame);
//			addedNode.setScore(minimaxValue(addedNode));
//		}
	}

//	private double minimaxValue(GameNode gameNode) {
//		Game theGame = gameNode.getGame();
//		// If terminated, then return score
//		if(theGame.isTerminated()) {
//			return theGame.getScore(isPlayer1);
//		}
//
//		Object[] moves = theGame.getMoves();
//		Game tempGame;
//		GameNode addedNode;
//
//
//		// Calculate the minimax value of all available moves
//		for(Object m: moves) {
//			tempGame = theGame.clone();
//			try{
//				tempGame.move(m);
//			} catch(InvalidMoveException exc) {
//				throw new IllegalArgumentException();
//				//continue;
//			}
//			addedNode = new GameNode(tempGame);
//			gameNode.addChild(addedNode);
//			addedNode.setScore(minimaxValue(addedNode));
//		}
//
//		// If my turn, return highest value of all of next moves
//		if(theGame.player1Turn() == isPlayer1) {
//			return Collections.max(gameNode.getChildren()).getScore();
//		}
//		// else (not my turn), return lowest value of all of next moves
//		else {
//			return Collections.min(gameNode.getChildren()).getScore();
//		}
//	}



}
