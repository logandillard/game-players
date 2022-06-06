package com.dillard.games.checkers.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.dillard.games.GamePlayer;
import com.dillard.games.checkers.CheckersGame;
import com.dillard.games.checkers.CheckersMove;

public class CheckersGUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private CheckersGame checkers;
	private CheckerBoardUI board;
//	private JFrame frame;

	public CheckersGUI (CheckersGame checkers, GamePlayer<CheckersMove, CheckersGame> player1, GamePlayer<CheckersMove, CheckersGame> player2) {
		this.checkers = checkers;

		init(player1, player2);
	}

	private void init(GamePlayer<CheckersMove, CheckersGame> player1, GamePlayer<CheckersMove, CheckersGame> player2) {
		this.setTitle("CheckerBoard");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(400, 150));

		board = new CheckerBoardUI(checkers, player1, player2, textArea);
		board.setSize(400, 400);


		JPanel topPanel = new JPanel();
		topPanel.setLayout(new FlowLayout());
		topPanel.add(new JLabel("Checkers"));
		topPanel.setSize(400, 50);

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout());
		bottomPanel.add(new JLabel("Buttons"));
		// TODO add new game button
//		bottomPanel.add(startButton);
//		bottomPanel.add(stopButton);
//		bottomPanel.add(debugButton);
//		bottomPanel.add(interactiveModeBox);
		bottomPanel.setSize(400, 50);

		// set a layout on the frame.
		// add some buttons and such

		// get the container info so we can write to the screen
		Container container = this.getContentPane();

		// create main screen
		container.setLayout(new BorderLayout(3, 3));
		container.add(board, BorderLayout.CENTER);

//		container.add(bottomPanel, BorderLayout.SOUTH);
		container.add(scrollPane, BorderLayout.SOUTH);


		container.add(topPanel, BorderLayout.NORTH);

		this.setSize(400,600);
		this.setResizable(false);
		this.setVisible(true);

		board.initPieces();
	}

	public void run() throws Exception {
		Thread.sleep(10000);
	}
}
