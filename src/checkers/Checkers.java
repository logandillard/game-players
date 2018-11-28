package checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import games.Game;
import games.InvalidMoveException;
import games.Move;

// Encapsulates all game logic
public class Checkers extends Observable implements Game {
	private static final int NUM_MOVES_NO_JUMPS_FOR_DRAW = 50;
	private static final boolean FORCED_JUMPS = true;

	int numMovesNoJumps = 0;
	private CheckersBoard board;
	boolean player1Turn;
	// NOTE player 1 == white
	PieceColor player1Color = PieceColor.WHITE;

	public Checkers() {
		board = new CheckersBoard();
		player1Turn = true;
	}
	private Checkers(CheckersBoard board, boolean player1Turn, int numMovesNoJumps) {
		this.board = board;
		this.player1Turn = player1Turn;
		this.numMovesNoJumps = numMovesNoJumps;
	}

	public Checkers clone() {
		return new Checkers(board.clone(), player1Turn, numMovesNoJumps);
	}


	private List<Move> availableMoves = null;

	@Override
	public void move(Move m) throws InvalidMoveException {
		if (!(m instanceof CheckersMove)) {
			throw new InvalidMoveException();
		}

		CheckersMove move = (CheckersMove) m;

		Piece piece = board.getPiece(move.getFrom());
		if (!isCurrentPlayerColor(piece.getColor())) {
			throw new InvalidMoveException("Wrong player's piece");
		}

		// check if move is an available move
		if (availableMoves == null) {
			getMoves();
		}
		if (!availableMoves.contains(m)) {
			throw new InvalidMoveException("Move is not currently a legal move: " + m);
		}

		try {	
			board.movePiece(move.getFrom(), move.getTo());
		} catch (BoardException e) {
			throw new InvalidMoveException(e);
		}

		if (move.hasJumpedPieces()) {
			try {
				for (CheckersLocation jumpedLocation : move.getJumpedPieces()) {
					board.removePiece(jumpedLocation);
				}
			} catch (BoardException e) {
				throw new InvalidMoveException(e);
			}
			numMovesNoJumps = 0;
		} else {
			numMovesNoJumps++;
		}
		
		// King moved piece if appropriate
		try {
			if (board.getPiece(move.getTo()).getColor() == PieceColor.WHITE) {
				if (move.getTo().getRow() == CheckersBoard.NUM_ROWS - 1) {
					board.kingPiece(move.getTo());
				}
			} else {
				if (move.getTo().getRow() == 0) {
					board.kingPiece(move.getTo());
				}
			}
		} catch (BoardException e) {
			throw new InvalidMoveException("Problem with kinging: " + e.getMessage());
		}

		player1Turn = !player1Turn;
		availableMoves = null;
		
		setChanged();
		notifyObservers();
	}
	
	public Piece[][] getBoardPieces() {
		return board.getBoardPieces();
	}

	@Override
	public List<Move> getMoves() {
		List<Move> moves = new ArrayList<Move>();
		List<Move> jumpMoves = new ArrayList<Move>();
		Piece[][] boardPieces  = board.getBoardPieces();

		for (int r=0; r < CheckersBoard.NUM_ROWS; r++) {
			for (int c=0; c < CheckersBoard.NUM_COLS; c++) {
				Piece tmpPiece = boardPieces[r][c];
				if (tmpPiece != null && isCurrentPlayerColor(tmpPiece.getColor())) {	
					addMoves(moves, jumpMoves, r, c, tmpPiece, jumpMoves.size() > 0, null);
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


	private void addMoves(List<Move> moves, List<Move> jumpMoves, int r, int c, Piece piece, 
			boolean onlyJump, List<CheckersLocation> jumpedLocations) {
		int signForward = piece.getColor() == player1Color ? 1 : -1;

		// Forward moves
		addMovesInRow(signForward, moves, jumpMoves, r, c, piece, onlyJump, jumpedLocations);

		// if king, backward moves
		if (piece.isKing()) {
			addMovesInRow(signForward * -1, moves, jumpMoves, r, c, piece, onlyJump, jumpedLocations);
		}
	}

	private void addMovesInRow(int signDir, List<Move> moves, List<Move> jumpMoves, int r, int c, Piece piece,
			boolean onlyJump, List<CheckersLocation> jumpedLocations) {
		int nextRow = r + signDir;
		if (CheckersBoard.isWithinBounds(nextRow)) {
			addMovesInRowCol(signDir, -1, moves, jumpMoves, r, c, piece, onlyJump, jumpedLocations);
			addMovesInRowCol(signDir, 1, moves, jumpMoves, r, c, piece, onlyJump, jumpedLocations);
		}
	}

	private void addMovesInRowCol(int rowSignDir, int colSignDir, List<Move> moves, List<Move> jumpMoves, 
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
				// otherwise, can move here. add jumped piece. check on further jumps
				// Don't re-jump pieces that we already jumped
				if (jumpedLocations == null) { jumpedLocations = new ArrayList<CheckersLocation>(); }
				jumpedLocations.add(newLocation);
				List<Move> furtherJumpMoves = new ArrayList<Move>();
				addMoves(null, furtherJumpMoves, jumpLocation.getRow(), jumpLocation.getCol(), 
						piece, true, jumpedLocations);
				if (furtherJumpMoves.size() == 0) {
					CheckersMove m = new CheckersMove(oldLocation, jumpLocation);
					m.addJumpedPiece(newLocation);
					furtherJumpMoves.add(m);
				} else {
					for (Move move : furtherJumpMoves) {
						((CheckersMove)move).setFrom(oldLocation);
						((CheckersMove)move).addJumpedPiece(newLocation);
					}
				}
				jumpMoves.addAll(furtherJumpMoves);
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


	@Override
	public int numMoves() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
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


	@Override
	public double evaluate(boolean player1) {
		if (player1) {
			return board.getNumWhitePieces() - board.getNumBlackPieces() + 
				(0.5 * (board.getNumWhiteKings() - board.getNumBlackKings()) );
		} else {
			return board.getNumBlackPieces() - board.getNumWhitePieces() + 
				(0.5 * (board.getNumBlackKings() - board.getNumWhiteKings()) );
		}
	}


	@Override
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
	public String toString() {
		return board.toString();
	}

}
