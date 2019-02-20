package com.dillard.games.checkers;

import com.dillard.games.InvalidMoveException;

public class CheckersTest {
	public static void main(String[] args) throws Exception {
		testGetMoves();

		testWrongPlayer();

		System.out.println("Everything OK.");
	}

	private static void testWrongPlayer() throws Exception {
		CheckersGame checkers = new CheckersGame();

		// Check initial player
		assertTrue(checkers.isPlayer1Turn(), "Right player starts");

		try {
			checkers.move(new CheckersMove(CheckersLocation.forLocation(5, 1), CheckersLocation.forLocation(4,2)));
			assertTrue(false, "Can't move the wrong player's piece");
		} catch (InvalidMoveException e) {
			// expected
		}

		checkers.move(new CheckersMove(CheckersLocation.forLocation(2, 4), CheckersLocation.forLocation(3,3)));
	}

	private static void testGetMoves() throws Exception {
		CheckersGame checkers = new CheckersGame();

		// Check initial moves
		//		System.out.println(checkers.toString());

		var moves = checkers.getMoves();
		assertEquals(7, moves.size(), "Right number of initial moves");


		// Check forced jump == 1 move
		checkers.move(new CheckersMove(CheckersLocation.forLocation(2, 4), CheckersLocation.forLocation(3,3)));
		checkers.move(new CheckersMove(CheckersLocation.forLocation(5, 1), CheckersLocation.forLocation(4,2)));

		// white has a jump
		moves = checkers.getMoves();
		assertEquals(1, moves.size(), "Right number of moves with forced jump");
		checkers.move(moves.get(0));
//		System.out.println(checkers);

		// black has a jump
		moves = checkers.getMoves();
		assertEquals(2, moves.size(), "Right number of moves with forced jump");
		checkers.move(moves.get(1));
//		System.out.println(checkers);

		// Set up a double jump
		// 2,6 to 3,7
		checkers.move(new CheckersMove(CheckersLocation.forLocation(2,6), CheckersLocation.forLocation(3,7)));
		// 5,5 to 4,4
		checkers.move(new CheckersMove(CheckersLocation.forLocation(5,5), CheckersLocation.forLocation(4,4)));
//		System.out.println(checkers);
		// 1,7 to 2,6
		checkers.move(new CheckersMove(CheckersLocation.forLocation(1,7), CheckersLocation.forLocation(2,6)));
		// 4,4 to 3,3
		checkers.move(new CheckersMove(CheckersLocation.forLocation(4,4), CheckersLocation.forLocation(3,3)));

		moves = checkers.getMoves();
		assertEquals(1, moves.size(), "Right number of moves with forced double jump");
		assertTrue(moves.get(0).jumpedLocation != null, "It is a jump move");
		boolean isPlayer1Turn = checkers.isPlayer1Turn();
//		assertEquals( , moves.get(0).jumpedLocation, "Has the jump location");
		checkers.move(moves.get(0));

		moves = checkers.getMoves();
		assertEquals(1, moves.size(), "Forced to complete the double jump");
		assertTrue(moves.get(0).jumpedLocation != null, "It is a jump move");
		assertTrue(isPlayer1Turn == checkers.isPlayer1Turn(), "Still the same player's turn");
//		System.out.println(checkers);
	}


	public static void assertEquals(int expected, int actual, String msg) {
		assertTrue(expected == actual, "Expected " + expected + " but got " + actual + " - " + msg);
	}
	public static void assertTrue(boolean value, String msg) {
		if (!value) throw new RuntimeException(msg);
	}
}
