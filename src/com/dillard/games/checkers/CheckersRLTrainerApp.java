package com.dillard.games.checkers;

import java.util.Random;

public class CheckersRLTrainerApp {

    public static void main(String[] args) {
        CheckersRLTrainer trainer = new CheckersRLTrainer(new Random(12345));
        System.out.println("Training");
        CheckersValueNN player = trainer.train();
        System.out.println("Finished training");

//        double result = trainer.evaluate(player);
//        System.out.println("Evaluation result: " + result);

    }
}
