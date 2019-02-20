package com.dillard.games.checkers;

public class CheckersDriver {

    public static double playNewGame(CheckersPlayer player1, CheckersPlayer player2, CheckersGame game) {
        if (game == null) {
            game = new CheckersGame();
        }

        while(!game.isTerminated()) {
            CheckersMove move;
            if (game.isPlayer1Turn()) {
                move = player1.move(game);
            } else {
                move = player2.move(game);
            }
            game.move(move);
        }

        double p1Score = game.getFinalScore(true);

        return p1Score;

//        double p2Score = game.getFinalScore(false);
//        if(p1Score > p2Score) {
//            if (player1 instanceof HumanPlayer) {
//                System.out.println("You win!!");
//            } else {
//                System.out.println("Player 1 wins!!");
//            }
//        } else if(p2Score > p1Score) {
//            if (player1 instanceof HumanPlayer) {
//                System.out.println("Computer player wins!!");
//            } else {
//                System.out.println("Player 2 wins!!");
//            }
//        } else {
//            System.out.println("Tie game!!");
//        }
    }
//
//	@Override
//	protected GamePlayer getPlayer1() {
//		return new HumanCheckersPlayer(iou);
//	}
//	@Override
//	protected GamePlayer getPlayer2() {
//		return new ABPruningPlayer<CheckersGame>(5);
////		return new HumanCheckersPlayer(iou);
//	}
//
//	@Override
//	protected String getPlayer1Message() {
//		return "White's turn";
//	}
//	@Override
//	protected String getPlayer2Message() {
//		return "Black's turn";
//	}
//
//	@Override
//	protected void welcome(IOUtilitiesInterface iou) {
//		iou.println("Welcome to checkers\n\n");
//	}
}
