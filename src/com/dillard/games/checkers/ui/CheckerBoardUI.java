package com.dillard.games.checkers.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.dillard.games.GamePlayer;
import com.dillard.games.InvalidMoveException;
import com.dillard.games.Move;
import com.dillard.games.checkers.CheckersGame;
import com.dillard.games.checkers.CheckersLocation;
import com.dillard.games.checkers.CheckersMove;
import com.dillard.games.checkers.Piece;
import com.dillard.games.checkers.PieceColor;

public class CheckerBoardUI extends JPanel implements ActionListener, MouseListener, Observer {
    private static final long serialVersionUID = 1L;
    private final int rows = 8, cols = 8;
	private final int boardWidthPx = 400, boardHeightPx = 400;
	private CheckersBoardSquare[][] panels;
	private CheckersGame model;
//	private CheckersLocation selectedLocation = null;
	private CheckersBoardSquare selectedSquare = null;
	private List<Move> currentMoves = null;
	private boolean isPlayer1Turn = true;
	GamePlayer<CheckersGame> player1;
	GamePlayer<CheckersGame> player2;
	JTextArea log;

	public CheckerBoardUI(CheckersGame model, GamePlayer<CheckersGame> player1, GamePlayer<CheckersGame> player2, JTextArea log) {
		super();
		this.model = model;
		model.addObserver(this);
		this.player1 = player1;
		this.player2 = player2;
		this.log = log;
		panels = new CheckersBoardSquare[rows][cols];

		setSize(boardWidthPx, boardHeightPx);

		setLayout(new GridLayout(rows,cols));

		// Create the grid background
		Color currentColor;
		boolean canHoldPieces;
		for (int x = 0; x <(rows*cols); x++) {
			int row = x / cols;
			int col = x % cols;

			int altr = 0;
			altr = (x) % cols;
			altr += (x) / cols;

			if (altr % 2 == 1) {
				currentColor = Color.white;
				canHoldPieces = false;
			} else {
				currentColor = new Color(150,150,150); //Color.black;
				canHoldPieces = true;
			}

			CheckersBoardSquare square = new CheckersBoardSquare(CheckersLocation.forLocation(row, col), canHoldPieces);
			square.setPreferredSize(new Dimension(boardWidthPx/cols, boardHeightPx/rows));
			square.setBackground(currentColor);
			square.addMouseListener(this);
			panels[row][col] = square;
		}

		for (int row = rows - 1; row >= 0; row--) {
			for (int col = 0; col < cols; col++) {
				add(panels[row][col]);
			}
		}

		currentMoves = model.getMoves();
		isPlayer1Turn = model.isPlayer1Turn();

//		repaint();
	}
//	public void paint(Graphics g) {
//		super.paint(g);
//	}

	public void initPieces() {
		// Create the checkers pieces
		for (int row = 0; row < 3; row ++) {
			for (int col = (row) % 2; col < cols; col+=2) {
				panels[row][col].addChecker(PieceColor.WHITE, false);
			}
		}

		for (int row = 5; row < rows; row ++) {
			for (int col = (row) % 2; col < cols; col+=2) {
				panels[row][col].addChecker(PieceColor.BLACK, false);
			}
		}
	}

	private void setPieces(Piece[][] boardPieces) {
		clearPieces();
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				if (boardPieces[row][col] != null) {
					panels[row][col].addChecker(boardPieces[row][col].getColor(), boardPieces[row][col].isKing());
				}
			}
		}
		repaint();
	}

	private void clearPieces() {
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				panels[row][col].removeChecker();
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO do I need this??
		// probably tell the model something
		int i=0;
		i++;
		if (i == 1) {
			return;
		}
	}

	@Override
	public void update(Observable observable, Object arg1) {
		// model tells me that something has changed.
		// get the necessary information and repaint the board as necessary
		if (observable instanceof CheckersGame) {
			CheckersGame checkers = (CheckersGame) observable;
			Piece[][] boardPieces = checkers.getBoardPieces();
			setPieces(boardPieces);
			currentMoves = null;

			// This isn't really supposed to be done, but it works, so hey
			paint(this.getGraphics());

			if (model.isTerminated()) {
			    log.append("The game is over.");
			    return;
			}

			if (model.isPlayer1Turn()) {
				if (player1 != null) {
					getAndDoMove(player1);
				}
			} else {
				if (player2 != null) {
					getAndDoMove(player2);
				}
			}
		}
	}

	private void getAndDoMove(GamePlayer<CheckersGame> player) {
		try {
			Move m = player.move(model);
			model.move(m);
		} catch (Exception e) {
			e.printStackTrace();
			log.append(e.toString());
		}
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		Component comp = event.getComponent();
		if (comp instanceof CheckersBoardSquare) {
			CheckersBoardSquare square = (CheckersBoardSquare) comp;
//			CheckersLocation location = square.getCheckersLocation();

			if (selectedSquare != null) {
				List<CheckersMove> fromMoves = canMoveFrom(selectedSquare);

				// Can we move from selected location to here?
				// makes sure it's the right player's turn
				CheckersMove m = canMoveTo(fromMoves, square);

				selectedSquare.toggleSelected();
				selectedSquare = null;

				if (m != null) {
					// Do the move
					try {
						model.move(m);
						currentMoves = null;
					} catch (InvalidMoveException e) {
						e.printStackTrace();
					}
				}

				if (m != null) return;
			}

			List<CheckersMove> fromMoves = canMoveFrom(square);

			if (fromMoves.size() > 0) {
				// makes sure it's the right player's turn
				selectedSquare = square;
				selectedSquare.toggleSelected();
			}

		}

	}


	@Override
	public void mouseClicked(MouseEvent event) {	}

	private CheckersMove canMoveTo(List<CheckersMove> fromMoves, CheckersBoardSquare to) {

		// Is the to appropriate?
		for (CheckersMove m : fromMoves) {
			// TODO this won't work with multiple jumps
			if (m.getTo().equals(to.getCheckersLocation())) {
				return m;
			}
		}

		return null;
	}

	private List<CheckersMove> canMoveFrom(CheckersBoardSquare from) {
		// Can the 'from' square be moved from?
		List<CheckersMove> fromMoves = new ArrayList<CheckersMove>();
		if (currentMoves == null) {
			currentMoves = model.getMoves();
		}
		for (Move m : currentMoves) {
			if (((CheckersMove)m).getFrom().equals(from.getCheckersLocation())) {
				fromMoves.add((CheckersMove)m);
			}
		}
		return fromMoves;
	}



	@Override
	public void mouseEntered(MouseEvent arg0) {}
	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mousePressed(MouseEvent arg0) {}
}