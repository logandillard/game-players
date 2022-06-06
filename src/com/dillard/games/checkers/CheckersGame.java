package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.HashMap;
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
	private static final int NUM_MOVES_NO_JUMPS_FOR_DRAW = 50;
	private static final int NUM_REPEATED_POSITIONS_FOR_DRAW = 3;
	private static final boolean FORCED_JUMPS = true;

	private CheckersBoard board;
	private boolean player1Turn;
	private List<CheckersMove> availableMoves = null;
	private int moveCount = 0;
	private int numMovesNoJumpsOrCrowns = 0;
	private Map<String, Integer> boardStateCounts = new HashMap<>();
	private int maxBoardStateCount = 1;
	// NOTE player 1 == white
	private static final PieceColor PLAYER_1_COLOR = PieceColor.WHITE;

	public CheckersGame() {
		this(true);
	}
    public CheckersGame(boolean isPlayer1Turn) {
        board = new CheckersBoard();
        player1Turn = isPlayer1Turn;
    }
	private CheckersGame(CheckersBoard board, boolean player1Turn,
	        List<CheckersMove> availableMoves, int numMovesNoJumpsOrCrowns,
	        Map<String, Integer> boardStateCounts, int maxBoardStateCount, int moveCount) {
		this.board = board;
		this.player1Turn = player1Turn;
		this.availableMoves = availableMoves;
		this.numMovesNoJumpsOrCrowns = numMovesNoJumpsOrCrowns;
		this.boardStateCounts = boardStateCounts;
		this.maxBoardStateCount = maxBoardStateCount;
		this.moveCount = moveCount;
	}

	@Override
    public CheckersGame clone() {
		return new CheckersGame(
		        board.clone(),
		        player1Turn,
		        availableMoves == null ? null : new ArrayList<>(availableMoves),
		        numMovesNoJumpsOrCrowns,
		        new HashMap<>(boardStateCounts),
		        maxBoardStateCount,
		        moveCount);
	}

	public CheckersBoard cloneBoard() {
	    return board.clone();
	}

	public void move(CheckersMove move) {
		Piece piece = board.getPiece(move.from);
		if (piece == null) {
		    throw new InvalidMoveException("No piece exists in this location");
		}

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
			numMovesNoJumpsOrCrowns = 0;

		    // Get further jump moves
			furtherJumpMoves = new ArrayList<>();
			addMoves(null, furtherJumpMoves, to.getRow(), to.getCol(), piece, true);
			if (furtherJumpMoves.isEmpty()) {
			    furtherJumpMoves = null;
			}

		} else {
			numMovesNoJumpsOrCrowns++;
		}

		// King moved piece if appropriate
		if (!board.getPiece(to).isKing()) {
    		if (board.getPiece(to).getColor() == PieceColor.WHITE) {
    			if (to.getRow() == CheckersBoard.NUM_ROWS - 1) {
    				board.kingPiece(move.to);
    				numMovesNoJumpsOrCrowns = 0;
    			}
    		} else {
    			if (to.getRow() == 0) {
    				board.kingPiece(move.to);
    				numMovesNoJumpsOrCrowns = 0;
    			}
    		}
		}

		if (furtherJumpMoves == null || furtherJumpMoves.isEmpty()) {
    		player1Turn = !player1Turn;
    		availableMoves = null;
		} else {
		    availableMoves = furtherJumpMoves;
		}

		if (!piece.isKing || move.jumpedLocation != null) {
		    // moving a non-king, or jumping, makes it so all past game states can never be repeated.
		    // clear the counts so that we do not have to clone them all
		    boardStateCounts.clear();
		}

		if (board.getNumKings() > 0) {
		    // only need to keep these counts if there are kings
		    updateBoardStateCounts();
		}

		moveCount++;

		setChanged();
		notifyObservers();
	}

	private void updateBoardStateCounts() {
        String boardStateStr = getBoardStateString();
	    Integer existing = boardStateCounts.get(boardStateStr);
	    if (existing == null) {
	        existing = 0;
	    }
	    existing++;
	    boardStateCounts.put(boardStateStr, existing);
	    if (existing > maxBoardStateCount) {
	        maxBoardStateCount = existing;
	    }
    }

    private String getBoardStateString() {
        var board = this.board.getBoardPieces();
        StringBuilder state = new StringBuilder();
//        for (int row=0; row<board.length; row++) {
//            for (int col=row % 2; col<board[row].length; col += 2) {
//                Piece p = board[row][col];
//                if (p == null) {
//                    state.append('0');
//                } else {
//                    if (p.isKing) {
//                        if (p.color == PieceColor.WHITE) {
//                            state.append('W');
//                        } else {
//                            state.append('B');
//                        }
//                    } else {
//                        if (p.color == PieceColor.WHITE) {
//                            state.append('w');
//                        } else {
//                            state.append('b');
//                        }
//                    }
//                }
//            }
//        }
        // only need to keep track of the kings because non-king moves reset the counts
        for (int row=0; row<board.length; row++) {
            for (int col=row % 2; col<board[row].length; col += 2) {
                Piece p = board[row][col];
                if (p != null && p.isKing) {
                    state.append(String.valueOf(row * 10 + col));
                    if (p.color == PieceColor.WHITE) {
                        state.append('W');
                    } else {
                        state.append('B');
                    }
                }
            }
        }
        return state.toString();
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
		int signForward = piece.color == PLAYER_1_COLOR ? 1 : -1;

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
    		numMovesNoJumpsOrCrowns >= NUM_MOVES_NO_JUMPS_FOR_DRAW ||
    		// or the current player has no moves
    		availableMoves.size() == 0 ||
    		// we have seen the same position > 3 times
    		maxBoardStateCount > NUM_REPEATED_POSITIONS_FOR_DRAW;
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

       if (numMovesNoJumpsOrCrowns >= NUM_MOVES_NO_JUMPS_FOR_DRAW) {
           return 0.0;
       }

       if (maxBoardStateCount > NUM_REPEATED_POSITIONS_FOR_DRAW) {
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

	public int getMoveCount() {
	    return moveCount;
	}

	@Override
    public String toString() {
		return board.toString();
	}
}
