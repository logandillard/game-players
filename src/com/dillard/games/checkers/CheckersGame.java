package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import com.dillard.games.Game;
import com.dillard.games.InvalidMoveException;
import com.dillard.games.checkers.MCTS.MCTSGame;

// Encapsulates all game logic
@SuppressWarnings("deprecation")
public class CheckersGame extends Observable implements
Game<CheckersMove, CheckersGame>, MCTSGame<CheckersMove, CheckersGame> {
	private static final int NUM_MOVES_NO_JUMPS_FOR_DRAW = 100;
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
			addMoves(null, furtherJumpMoves, to.getRow(), to.getCol(), piece, true);
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
	    if (availableMoves != null) {
	        return availableMoves;
	    }

		List<CheckersMove> moves = new ArrayList<>();
		List<CheckersMove> jumpMoves = new ArrayList<>();
		Piece[][] boardPieces  = board.getBoardPieces();

		PieceColor currentPlayerColor = getCurrentPlayerColor();
		for (int r=0; r < CheckersBoard.NUM_ROWS; r++) {
			for (int c=r%2; c < CheckersBoard.NUM_COLS; c+=2) {
				Piece p = boardPieces[r][c];
				if (p != null && currentPlayerColor == p.color) {
					addMoves(moves, jumpMoves, r, c, p, jumpMoves.size() > 0);
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
			boolean onlyJump) {
		int signForward = piece.color == player1Color ? 1 : -1;

		// Forward moves
		addMovesInRow(signForward, moves, jumpMoves, r, c, piece, onlyJump);

		// if king, backward moves
		if (piece.isKing) {
			addMovesInRow(signForward * -1, moves, jumpMoves, r, c, piece, onlyJump);
		}
	}

	private void addMovesInRow(int signDir, List<CheckersMove> moves, List<CheckersMove> jumpMoves, int r, int c, Piece piece,
			boolean onlyJump) {
		int nextRow = r + signDir;
		if (CheckersBoard.isWithinBounds(nextRow)) {
			addMovesInRowCol(signDir, -1, moves, jumpMoves, r, c, piece, onlyJump);
			addMovesInRowCol(signDir,  1, moves, jumpMoves, r, c, piece, onlyJump);
		}
	}

	private void addMovesInRowCol(int rowSignDir, int colSignDir, List<CheckersMove> moves, List<CheckersMove> jumpMoves,
			int r, int c, Piece piece, boolean onlyJump) {
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
		}  else { // Location is occupied by opposite color
			// - make sure we haven't already jumped this piece

			// Try the jump location
		    int jumpRow = newRow + rowSignDir;
		    int jumpCol = newCol + colSignDir;
		    // If not on the board, then nevermind (return)
		    if (!CheckersBoard.isWithinBounds(jumpRow)) return;
		    if (!CheckersBoard.isWithinBounds(jumpCol)) return;
			CheckersLocation jumpLocation = CheckersLocation.forLocation(newRow + rowSignDir, newCol + colSignDir);
			// If not empty, nevermind (return)
			if (!board.isLocationEmpty(jumpLocation)) {
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

	private PieceColor getCurrentPlayerColor() {
        if (isPlayer1Turn()) {
            return PieceColor.WHITE;
        } else {
            return PieceColor.BLACK;
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

	public double evaluatePieceCount(boolean player1) {
		if (player1) {
			return board.getNumWhitePieces() - board.getNumBlackPieces() +
				(0.5 * (board.getNumWhiteKings() - board.getNumBlackKings()) );
		} else {
			return board.getNumBlackPieces() - board.getNumWhitePieces() +
				(0.5 * (board.getNumBlackKings() - board.getNumWhiteKings()) );
		}
	}

	public double evaluate(boolean player1) {
	    return evaluatePieceCount(player1);
	}

	public int getMinPlayerPieceCount() {
	    return Math.min(board.getNumWhitePieces(), board.getNumBlackPieces());
	}

	public double getFinalScore(boolean player1) {
       if (availableMoves == null) {
            getMoves();
        }

       if (board.getNumWhitePieces() == 0) {
           if (player1) {
               return -1.0;
           } else {
               return 1.0;
           }
       }
       if (board.getNumBlackPieces() == 0) {
           if (player1) {
               return 1.0;
           } else {
               return -1.0;
           }
       }

       if (availableMoves.isEmpty()) {
           if (isPlayer1Turn() == player1) {
               return -1.0;
           } else {
               return 1.0;
           }
       }

       if (numMovesNoJumps >= NUM_MOVES_NO_JUMPS_FOR_DRAW) {
           return 0.0;
       }

       throw new RuntimeException("Called getFinalScore, but the game is not terminated");
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
