package com.dillard.games.herorealms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HRMainInteractive {
    public static void main(String[] args) throws Exception {
        System.out.println("STARTING");

        List<HRPlayer> players = new ArrayList<>();
        players.add(new HRPlayerHuman("Player1"));

        NNHRPlayer nnPlayer = new NNHRPlayer("NNPlayer", 456);
        nnPlayer.loadModel(NNPlayerTrainer.nnModelFileBest);
        nnPlayer.setLearningMode(false);
        nnPlayer.setLogDebug(true);
        nnPlayer.setMakeRandomMoves(false);
        players.add(nnPlayer);

        playOneGame(players, true);

        System.out.println("DONE");
    }

    private static void playOneGame(List<HRPlayer> players, boolean logChanges) {
        // start game
//        long seed = 1614893934399l;
        long seed = System.currentTimeMillis();
        System.out.println("Random seed is: " + seed);
        Collections.shuffle(players);
        HRGame game = new HRGame(players, seed);
        game.setLogChanges(logChanges);
        game.play();

        System.out.println("Winner is: " + game.getWinner());
        System.out.println("Time taken (ms): " + (System.currentTimeMillis() - seed));
    }
}
