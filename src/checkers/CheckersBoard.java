package checkers;

import java.util.Map;

// Abstracts away the representation of the board. Doesn't know game logic/rules.
public class CheckersBoard implements Cloneable {
	static final int NUM_ROWS = 8;
	static final int NUM_COLS = 8;
	
	private Piece[][] boardPieces;
	private int numPieces = 0;
	private int numWhitePieces = 0;
	private int numBlackPieces = 0;
	private int numWhiteKings = 0;
	private int numBlackKings = 0;
	
	CheckersBoard() {
		boardPieces = initializeBoard();
	}
	private CheckersBoard(Piece[][] boardPieces, int numPieces, int numWhitePieces, int numBlackPieces, 
			int numWhiteKings, int numBlackKings) {
		this.boardPieces = boardPieces;
		this.numPieces = numPieces;
		this.numWhitePieces = numWhitePieces;
		this.numBlackPieces = numBlackPieces;
		this.numWhiteKings = numWhiteKings;
		this.numBlackKings = numBlackKings;
	}
	
	public void movePiece(CheckersLocation from, CheckersLocation to) throws BoardException {
		
		Piece p = boardPieces[from.getRow()][from.getCol()];
		Piece existingDest = boardPieces[to.getRow()][to.getCol()];
		if (existingDest != null) {
			throw new BoardException();
		}
		boardPieces[to.getRow()][to.getCol()] = p;
		boardPieces[from.getRow()][from.getCol()] = null;
	}
	
	public void kingPiece(CheckersLocation location) throws BoardException {
		int row = location.getRow(), col = location.getCol();
		
		Piece p = boardPieces[row][col];
		if (p == null) throw new BoardException();
		if (p.isKing()) {
			if (p.getColor() == PieceColor.WHITE) {
				numWhiteKings--;
			} else {
				numBlackKings--;
			}
		}
		boardPieces[row][col] = Piece.forValue(p.getColor(), true);
	}
	
	public void removePiece(CheckersLocation location) throws BoardException {
		int row = location.getRow();
		int col = location.getCol();
		
		Piece existingPiece = boardPieces[row][col];
		if (existingPiece == null) {
			throw new BoardException();
		}
		boardPieces[row][col] = null;
		numPieces--;
		decrementPieceCount(existingPiece);
	}
	
	public boolean isLocationEmpty(CheckersLocation location) {
		return getPiece(location) == null;
	}
	public Piece getPiece(CheckersLocation location) {
		return boardPieces[location.getRow()][location.getCol()];
	}
	
	
	private void decrementPieceCount(Piece p) {
		if (p.getColor() == PieceColor.WHITE) {
			numWhitePieces--;
			if (p.isKing()) numWhiteKings--;
		} else {
			numBlackPieces--;
			if (p.isKing()) numBlackKings--;
		}
	}
	
	private Piece[][] initializeBoard() {
		Piece[][] boardPieces = new Piece[NUM_ROWS][NUM_COLS];
		
		for (int row=0; row<3; row++) {
			int col = 0;
			if (!isEven(row)) {
				col++;
			}
			
			for (; col < NUM_COLS; col += 2) {
				boardPieces[row][col] = Piece.forValue(PieceColor.WHITE, false);
				numPieces++;
				numWhitePieces++;
			}
		}
		
		for (int row=5; row<8; row++) {
			int col = 0;
			if (!isEven(row)) {
				col++;
			}
			
			for (; col < NUM_COLS; col += 2) {
				boardPieces[row][col] = Piece.forValue(PieceColor.BLACK, false);
				numPieces++;
				numBlackPieces++;
			}
		}
		
		return boardPieces;
	}
	
	public CheckersBoard clone() {
		Piece[][] boardPieces = new Piece[NUM_ROWS][NUM_COLS];
		for (int r = 0; r < NUM_ROWS; r++) {
			for (int c = 0; c < NUM_COLS; c++) {
				boardPieces[r][c] = this.boardPieces[r][c] == null ? null : this.boardPieces[r][c].clone();
			}
		}
		
		return new CheckersBoard(boardPieces, numPieces, numWhitePieces, numBlackPieces, numWhiteKings, numBlackKings);
	}
	
	public boolean isLegalPosition(int row, int col) {
		// if row is even, col must be odd
		// if row is odd, col must be even
		
		if (isEven(row)) {
			return !isEven(col);
		} else {
			return isEven(col);
		}
	}
	
	private boolean isEven(int i) {
		return i % 2 == 0;
	}
	
	private static String NO_PIECE = " ";
	private static String WHITE_PIECE = "w";
	private static String WHITE_PIECE_KING = "W";
	private static String BLACK_PIECE = "b";
	private static String BLACK_PIECE_KING = "B";
	public String toString() {
		return toString(null);
	}
	public String toString(Map<CheckersLocation, String> locationReplacements) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(" ");
		for (int col = 0; col < NUM_COLS; col++) {sb.append("-");}
		sb.append("\n");
		
		for (int row = NUM_ROWS - 1; row >= 0; row--) {
			sb.append("|");
			for (int col = 0; col < NUM_COLS; col++) {
				Piece piece = boardPieces[row][col]; 
				if (piece == null) {
					sb.append(NO_PIECE);
				} else {
					String representation = null;
					
					if (locationReplacements != null) {
						CheckersLocation thisLoc = CheckersLocation.forLocation(row, col);
						if (locationReplacements.containsKey(thisLoc)) {
							representation = locationReplacements.get(thisLoc);
						}
					}
					
					if (representation == null) {
						if (piece.getColor() == PieceColor.WHITE) {
							if (piece.isKing()) representation = WHITE_PIECE_KING;
							else representation = WHITE_PIECE;
						} else {
							if (piece.isKing()) representation = BLACK_PIECE_KING;
							else representation = BLACK_PIECE;
						}
					}
					
					sb.append(representation);
				}
			}
			sb.append("|");
			sb.append("\n");
		}
		
		sb.append(" ");
		for (int col = 0; col < NUM_COLS; col++) {sb.append("-");}
		
		return sb.toString();
	}

	public static boolean isWithinBounds(int row) {
		return row >= 0 && row < NUM_ROWS;
	}
	public static boolean isWithinBounds(CheckersLocation l) {
		if (l == null) {
			return false;
		}
		return isWithinBounds(l.getRow()) && isWithinBounds(l.getCol());
	}
	
	public Piece[][] getBoardPieces() {
		return boardPieces;
	}
	public int getNumPieces() {
		return numPieces;
	}
	public int getNumWhitePieces() {
		return numWhitePieces;
	}
	public int getNumBlackPieces() {
		return numBlackPieces;
	}
	public int getNumWhiteKings() {
		return numWhiteKings;
	}
	public int getNumBlackKings() {
		return numBlackKings;
	}
}
