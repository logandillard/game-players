package com.dillard.games;

public class InvalidMoveException extends Exception {
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
