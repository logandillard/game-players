package com.dillard.games.risk;

public class InvalidMoveException extends Exception {
	private static final long serialVersionUID = 1L;

	public InvalidMoveException(String s) {
		super(s);
	}
	
	public InvalidMoveException() {
		super();
	}
	
	public InvalidMoveException(Exception e) {
		super(e);
	}
}
