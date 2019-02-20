package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import com.dillard.games.InvalidMoveException;
import com.dillard.games.checkers.MCTS.MCTSGame;

// Encapsulates all game logic
@SuppressWarnings("deprecation")
public class CheckersGame extends Observable implements MCTSGame<CheckersMove, CheckersGame> {
	private static final int NUM_MOVES_NO_JUMPS_FOR_DRAW = 50;
	private static final boolean FORCED_JUMPS = true;

	int numMovesNoJumps = 0;
	private CheckersBoard board;
	boolean player1Turn;
	// NOTE player 1 == white
	PieceColor player1Color = PieceColor.WHITE;

	public CheckersGame() {
		this(true);
	}
    public CheckersGame(boolean isPlayer1Turn) {
        board = new CheckersBoard();
        player1Turn = isPlayer1Turn;
    }
	private CheckersGame(CheckersBoard board, boolean player1Turn, int numMovesNoJumps) {
		this.board = board;
		this.player1Turn = player1Turn;
		this.numMovesNoJumps = numMovesNoJumps;
	}

	@Override
    public CheckersGame clone() {
		return new CheckersGame(board.clone(), player1Turn, numMovesNoJumps);
	}

	public CheckersBoard cloneBoard() {
	    return board.clone();
	}

	private List<CheckersMove> availableMoves = null;

	public void move(CheckersMove move) {
		Piece piece = board.getPiece(move.from);
		if (!isCurrentPlayerColor(piece.getColor())) {
			throw new InvalidMoveException("Wrong player's piece");
		}

		// check if move is an available move
		if (availableMoves == null) {
			getMoves();
		}
		if (!availableMoves.contains(move)) {
			throw new InvalidMoveException("Move is not currently a legal move: " + move);
		}

        CheckersLocation to = move.to;

		try {
			board.movePiece(move.from, to);
		} catch (BoardException e) {
			throw new InvalidMoveException(e);
		}

		List<CheckersMove> furtherJumpMoves = null;
		if (move.jumpedLocation != null) {
			try {
				board.removePiece(move.jumpedLocation);
			} catch (BoardException e) {
				throw new InvalidMoveException(e);
			}
			numMovesNoJumps = 0;

		    // Get further jump moves
			furtherJumpMoves = new ArrayList<>();
			addMoves(null, furtherJumpMoves, to.getRow(), to.getCol(), piece, true, null);
			if (furtherJumpMoves.isEmpty()) {
			    furtherJumpMoves = null;
			}

		} else {
			numMovesNoJumps++;
		}

		// King moved piece if appropriate
		if (!board.getPiece(to).isKing()) {
    		if (board.getPiece(to).getColor() == PieceColor.WHITE) {
    			if (to.getRow() == CheckersBoard.NUM_ROWS - 1) {
    				board.kingPiece(move.to);
    			}
    		} else {
    			if (to.getRow() == 0) {
    				board.kingPiece(move.to);
    			}
    		}
		}

		if (furtherJumpMoves == null || furtherJumpMoves.isEmpty()) {
    		player1Turn = !player1Turn;
    		availableMoves = null;
		} else {
		    availableMoves = furtherJumpMoves;
		}

		setChanged();
		notifyObservers();
	}

	public Piece[][] getBoardPieces() {
		return board.getBoardPieces();
	}

	public List<CheckersMove> getMoves() {
		List<CheckersMove> moves = new ArrayList<>();
		List<CheckersMove> jumpMoves = new ArrayList<>();
		Piece[][] boardPieces  = board.getBoardPieces();

		for (int r=0; r < CheckersBoard.NUM_ROWS; r++) {
			for (int c=r%2; c < CheckersBoard.NUM_COLS; c+=2) {
				Piece p = boardPieces[r][c];
				if (p != null && isCurrentPlayerColor(p.getColor())) {
					addMoves(moves, jumpMoves, r, c, p, jumpMoves.size() > 0, null);
				}
			}
		}

		if (FORCED_JUMPS && jumpMoves.size() > 0) {
			availableMoves = jumpMoves;
			return jumpMoves;
		} else {
			moves.addAll(jumpMoves);
			availableMoves = moves;
			return moves;
		}
	}


	private void addMoves(List<CheckersMove> moves, List<CheckersMove> jumpMoves, int r, int c, Piece piece,
			boolean onlyJump, List<CheckersLocation> jumpedLocations) {
		int signForward = piece.getColor() == player1Color ? 1 : -1;

		// Forward moves
		addMovesInRow(signForward, moves, jumpMoves, r, c, piece, onlyJump, jumpedLocations);

		// if king, backward moves
		if (piece.isKing()) {
			addMovesInRow(signForward * -1, moves, jumpMoves, r, c, piece, onlyJump, jumpedLocations);
		}
	}

	private void addMovesInRow(int signDir, List<CheckersMove> moves, List<CheckersMove> jumpMoves, int r, int c, Piece piece,
			boolean onlyJump, List<CheckersLocation> jumpedLocations) {
		int nextRow = r + signDir;
		if (CheckersBoard.isWithinBounds(nextRow)) {
			addMovesInRowCol(signDir, -1, moves, jumpMoves, r, c, piece, onlyJump, jumpedLocations);
			addMovesInRowCol(signDir,  1, moves, jumpMoves, r, c, piece, onlyJump, jumpedLocations);
		}
	}

	private void addMovesInRowCol(int rowSignDir, int colSignDir, List<CheckersMove> moves, List<CheckersMove> jumpMoves,
			int r, int c, Piece piece, boolean onlyJump, List<CheckersLocation> jumpedLocations) {
		// If location is off board, return
		int newCol = colSignDir + c;
		if (!CheckersBoard.isWithinBounds(newCol)) return;

		// If location is empty, add move for this location, unless we're only looking for jump moves
		int newRow = r + rowSignDir;
		CheckersLocation oldLocation = CheckersLocation.forLocation(r, c);
		CheckersLocation newLocation = CheckersLocation.forLocation(newRow, newCol);
		if (board.isLocationEmpty(newLocation)) {
			if (!onlyJump) {
				moves.add(new CheckersMove(oldLocation, newLocation));
			}
			return;
		}

		// If location is occupied by same color, return
		Piece existingPiece = board.getPiece(newLocation);
		if (existingPiece.getColor() == piece.getColor()) {
			return;
		}  else if (jumpedLocations == null || !jumpedLocations.contains(newLocation)){ // Location is occupied by opposite color
			// - make sure we haven't already jumped this piece

			// Try the jump location
			CheckersLocation jumpLocation = CheckersLocation.forLocation(newRow + rowSignDir, newCol + colSignDir);
			// If not on board and empty, nevermind (return)
			if (!CheckersBoard.isWithinBounds(jumpLocation) || !board.isLocationEmpty(jumpLocation) ||
					(jumpedLocations != null && jumpedLocations.contains(jumpLocation)) ) {
				return;
			} else {
			    // we can jump to this location
			    CheckersMove m = new CheckersMove(oldLocation, jumpLocation, newLocation);
			    jumpMoves.add(m);

			    // Below was with multiple jumps in one move. I took that out to make moves simpler.
//				// otherwise, can move here. add jumped piece. check on further jumps
//				// Don't re-jump pieces that we already jumped
//				if (jumpedLocations == null) {
//				    jumpedLocations = new ArrayList<CheckersLocation>();
//				}
//				jumpedLocations.add(newLocation);
//
//				List<CheckersMove> furtherJumpMoves = new ArrayList<>();
//				addMoves(null, furtherJumpMoves, jumpLocation.getRow(), jumpLocation.getCol(),
//						piece, true, jumpedLocations);
//				if (furtherJumpMoves.size() == 0) {
//					CheckersMove m = new CheckersMove(oldLocation, jumpLocation);
//					m.addJumpedPiece(newLocation);
//					furtherJumpMoves.add(m);
//				} else {
//					for (CheckersMove move : furtherJumpMoves) {
//						move.setFrom(oldLocation);
//						move.addJumpedPiece(newLocation);
//					}
//				}
//				jumpMoves.addAll(furtherJumpMoves);
			}
		}
	}


	private boolean isCurrentPlayerColor(PieceColor color) {
		if (isPlayer1Turn()) {
			return color == PieceColor.WHITE;
		} else {
			return color == PieceColor.BLACK;
		}
	}


	public boolean isTerminated() {
		if (availableMoves == null) {
			getMoves();
		}

		// someone has lost
		return board.getNumWhitePieces() == 0 || board.getNumBlackPieces() == 0 ||
    		// or it's time for a draw
    		numMovesNoJumps >= NUM_MOVES_NO_JUMPS_FOR_DRAW ||
    		// or the current player has no moves
    		availableMoves.size() == 0 ;
	}

	public double evaluate(boolean player1) {
		if (player1) {
			return board.getNumWhitePieces() - board.getNumBlackPieces() +
				(0.5 * (board.getNumWhiteKings() - board.getNumBlackKings()) );
		} else {
			return board.getNumBlackPieces() - board.getNumWhitePieces() +
				(0.5 * (board.getNumBlackKings() - board.getNumWhiteKings()) );
		}
	}


	public double getFinalScore(boolean player1) {
		double evalScore = evaluate(player1);
		if (evalScore > 0) {
			return 1;
		} if (evalScore < 0) {
			return -1;
		} else {
			return 0;
		}
	}

	public boolean isPlayer1Turn() {
		return player1Turn;
	}

	public String toString(Map<CheckersLocation, String> locationReplacements) {
		return board.toString(locationReplacements);
	}

	@Override
    public String toString() {
		return board.toString();
	}
}
