package com.dillard.games.herorealms;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.dillard.nn.ActivationFunctionSeLU;
import com.dillard.nn.ActivationFunctionTanH;
import com.dillard.nn.LayeredNNTD;
import com.dillard.nn.NNLayerFullyConnectedTD;
import com.dillard.nn.WeightInitializerGaussianFixedVariance;

public final class NNHRPlayer implements HRPlayer {
    private double LEARNING_RATE = 0.00003; //0.001; TanH: 0.001 to 0.0003
    private double ELIG_DECAY_RATE = 0.3; // this is much better than 0.1 or 0.5. Could try more values.
    private double L2 = 0.00001;
    private static final int NUM_FEATURES = 319;
    private String name;
    private LayeredNNTD net;
    private LayeredNNTD targetNet;
    private boolean doDoubleQ = false;
    private double lastScore = 0;
    private boolean haveMadeAMove = false;
    private boolean learningMode = false;
    private Random random;
    private double randomMoveProb = 0.05;
    private boolean makeRandomMoves = true;
    private boolean logDebug = false;
    private DecimalFormat df3 = new DecimalFormat("0.000");

    public NNHRPlayer(String name, long seed) {
        boolean SELU_INITIALIZATION = false;
        boolean SELU_ACTIVATION = true;
        if (SELU_INITIALIZATION) {
        this.net = LayeredNNTD.buildFullyConnectedSELU(
                new int[] {NUM_FEATURES, 16, 16, 8, 1},
                new ActivationFunctionTanH(),
                LEARNING_RATE,
                ELIG_DECAY_RATE,
                L2,
                0.0    // L1 regularization
                );
        } else if (SELU_ACTIVATION) {
            this.net = LayeredNNTD.buildFullyConnected(
                    new int[] {NUM_FEATURES, 16, 16, 8, 1},
                    new ActivationFunctionSeLU(),
                    new WeightInitializerGaussianFixedVariance(Math.sqrt(2.0/350.0)),
                    LEARNING_RATE,
                    ELIG_DECAY_RATE,
                    L2,
                    0.0    // L1 regularization
                    );
            NNLayerFullyConnectedTD[] layers = net.getLayers();
            layers[layers.length-1].setActivationFunction(new ActivationFunctionTanH());
        } else {
            this.net = LayeredNNTD.buildFullyConnected(
                    new int[] {NUM_FEATURES, 16, 16, 8, 1},
                    new ActivationFunctionTanH(),
                    new WeightInitializerGaussianFixedVariance(Math.sqrt(2.0/350.0)),
                    LEARNING_RATE,
                    ELIG_DECAY_RATE,
                    L2,
                    0.0    // L1 regularization
                    );
        }

        this.cloneTargetNetwork();

        this.name = name;
        this.random = new Random(seed);
        this.learningMode = true;
    }

    public NNHRPlayer(String name, LayeredNNTD net) {
        this.name = name;
        this.net = net;
        this.learningMode = false;
        this.cloneTargetNetwork();
    }

    @Override
    public Card chooseBuy(List<Card> cards, HRGameState gameState, int gold) {
        if (logDebug) System.out.println("Choosing cards to buy");
        return chooseCard(cards, gameState, true, false, false);
    }

    @Override
    public Card chooseSacrifice(List<Card> cards, HRGameState gameState) {
        if (logDebug) System.out.println("Choosing cards to sacrifice");
        return chooseCard(cards, gameState, false, true, false);
    }

    @Override
    public Card chooseSacForDamage(List<Card> cards, HRGameState gameState) {
        if (logDebug) System.out.println("Choosing cards to sac for damage");
        return chooseCard(cards, gameState, false, false, true);
    }

    double[] lastMoveFeatures = null;
    public Card chooseCard(List<Card> cards, HRGameState gameState, boolean isBuy, boolean isSac, boolean isSacDmg) {
        double[] baseFeatures = createFeatures(gameState, null, null, null);
        double nullScore = score(baseFeatures);

        if (logDebug) {
            System.out.println(df3.format(nullScore) + " NONE");
        }

        double bestScore = nullScore;
        Card bestScoreCard = null;
        double[] bestScoreFeatures = baseFeatures;

        for (Card c : cards) {
            double[] features = createFeatures(
                    gameState,
                    isBuy ? c : null,
                    isSac ? c : null,
                    isSacDmg ? c : null);
            double score = score(features);

            if (logDebug) {
                System.out.println(df3.format(score) + " " + c);
            }

            if (score > bestScore) {
                bestScore = score;
                bestScoreCard = c;
                bestScoreFeatures = features;
            }
        }

        if (learningMode) {
            // Re-score the best move to get the activation levels for setting eligibility traces,
            // and to get the score for the current move
            double currentScore = score(bestScoreFeatures);

            if (haveMadeAMove) {
                // learn on the difference between the current move's score and the last move's score,
                // if there was a last move
                double targetScore = currentScore;
                if (doDoubleQ) {
                    targetScore = targetNet.activate(bestScoreFeatures)[0];
                }
                net.tdLearn(new double[] {targetScore - lastScore});

                currentScore = score(bestScoreFeatures);

                // TODO remove, just for debugging
//              double lastScoreAfterLearn = score(lastMoveFeatures);
//              double scoreDiff = lastScoreAfterLearn - lastScore;
//              System.out.println("Error :" + (currentScore - lastScore) +
//                      " Score diff: " + scoreDiff +
//                      " Last score " + lastScore +
//                      " Last score after learn " + lastScoreAfterLearn +
//                      " currentScore " + currentScore
//                      );
            }
            lastMoveFeatures = bestScoreFeatures;

            // save the current score for next time
            lastScore = currentScore;

            // update eligibility traces for this move
            net.updateElig();

            haveMadeAMove = true;

            if (makeRandomMoves && random.nextDouble() < randomMoveProb) {
                resetLearning(); // TODO is this good?
                List<Card> cardsWithNull = new ArrayList<>(cards);
                cardsWithNull.add(null);
                return cardsWithNull.get(random.nextInt(cardsWithNull.size()));
            }
        }

        if (logDebug) System.out.println("Chose " + bestScoreCard);

        return bestScoreCard;
    }

    @Override
    public Card chooseTuck(List<Card> cardsToTuck, HRGameState gameState, int gold) {
        return cardsToTuck.stream().min((c1, c2) -> c1.cost - c2.cost).get();
    }

    private double[] createFeatures(HRGameState gameState, Card boughtCard, Card sacrificedCard, Card sacForDamageCard) {
        HRPlayer opponent = gameState.currentOpponent();
        if (opponent == this) throw new RuntimeException();
        List<Card> library = new ArrayList<>(gameState.getLibrary(this));
        List<Card> oppoLibrary = new ArrayList<>(gameState.getLibrary(opponent));
        int turnNumber = gameState.getTurnNumber();
        List<Card> allCards = new ArrayList<>(gameState.getAllPlayerCards(this));
        List<Card> oppoAllCards = new ArrayList<>(gameState.getAllPlayerCards(opponent));
        List<Card> market = new ArrayList<>(gameState.getMarket());

        if (boughtCard != null) {
            allCards.add(boughtCard);
        }
        if (sacrificedCard != null) {
            allCards.remove(sacrificedCard);
        }
        if (sacForDamageCard != null) {
            allCards.remove(sacForDamageCard);
        }
        int additionalSacDamage = sacForDamageCard == null ? 0 : sacForDamageCard.sacrificeDamage();

        int[] colorCount = Cards.colorCounts(allCards);
        int[] oppoColorCount = Cards.colorCounts(oppoAllCards);

        int countZeroCost = 0;
        double avgCost = 0;
        for (Card c: allCards) {
            if (c.cost == 0) {
                countZeroCost++;
            } else {
                avgCost += c.cost;
            }
        }
        avgCost /= allCards.size();

        int oppoCountZeroCost = 0;
        double oppoAvgCost = 0;
        for (Card c: oppoAllCards) {
            if (c.cost == 0) {
                oppoCountZeroCost++;
            } else {
                oppoAvgCost += c.cost;
            }
        }
        oppoAvgCost /= oppoAllCards.size();

        double[] features = newFeatureArray();

        int idx = 0;

        features[idx++] = turnNumber / 10.0;
        // Health
        features[idx++] = gameState.getPlayerHealth(this) / 50.0;
        features[idx++] = (gameState.getPlayerHealth(opponent) - additionalSacDamage) / 50.0;
        // Card count
        features[idx++] = (allCards.size()) / 20.0; // TODO what about cards in play?
        features[idx++] = (oppoAllCards.size()) / 20.0;

        features[idx++] = countZeroCost / 10.0;
        features[idx++] = avgCost / 3.0;

        features[idx++] = oppoCountZeroCost / 10.0;
        features[idx++] = oppoAvgCost / 3.0;

        // Color counts
        for (int i=0; i<colorCount.length; i++) {
            features[idx++] = colorCount[i] / (double) allCards.size();
        }
        for (int i=0; i<oppoColorCount.length; i++) {
            features[idx++] = oppoColorCount[i] / (double) oppoAllCards.size();
        }

        // Individual cards total
        for (Card c : allCards) {
            features[idx + c.ordinal]++;
        }
        idx += Cards.NUM_UNIQUE_CARDS;

        // Individual cards in library (not in discard pile)
        for (Card c : library) {
            features[idx + c.ordinal]++;
        }
        idx += Cards.NUM_UNIQUE_CARDS;

        // Opponent individual cards
        for (Card c : oppoAllCards) {
            features[idx + c.ordinal]++;
        }
        idx += Cards.NUM_UNIQUE_CARDS;

        // Opponent individual cards in library (not in discard pile)
        for (Card c : oppoLibrary) {
            features[idx + c.ordinal]++;
        }
        idx += Cards.NUM_UNIQUE_CARDS;

        // Champions // TODO
//        List<Card> champions = gameState.getChampions(this);

        // Market
        for (Card c: market) {
            if (c != boughtCard) {
                features[idx + c.ordinal]++;
            }
        }
        idx += Cards.NUM_UNIQUE_CARDS;


        return features;
    }

    private double[] newFeatureArray() {
        return new double[NUM_FEATURES];
    }

    public void learnFinalResult(boolean isWinner, boolean isDraw, HRGameState gameState) {
        if (!learningMode) {
            throw new RuntimeException();
        }

        double score = isWinner ? 1.0 : -1.0;
        if (isDraw) {
            score = 0.5;
        }
        net.tdLearn(new double[] {score - lastScore});

        // TODO remove, just for debugging
//        double lastScoreAfterLearn = score(lastMoveFeatures);
//        double scoreDiff = lastScoreAfterLearn - lastScore;
//        System.out.println("Error :" + (score - lastScore) +
//            " Score diff: " + scoreDiff +
//            " Last score " + lastScore +
//            " Last score after learn " + lastScoreAfterLearn +
//            " currentScore " + score
//            );
//        System.out.println(String.format("Final score: %s", df3.format(score)));
    }

    public void resetLearning() {
        net.reset(); // reset eligibility traces
    }

    private double score(double[] features) {
        return net.activate(features)[0];
    }

    public void cloneTargetNetwork() {
        targetNet = new LayeredNNTD(net);
    }

    public LayeredNNTD getNeuralNet() {
        return net;
    }

    public double getLastScore() {
        return lastScore;
    }

    public void setLearningMode(boolean doLearn) {
        learningMode = doLearn;
    }

    public void saveModel(String file) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(net);
        oos.close();
    }

    public void loadModel(String file) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        net = (LayeredNNTD)ois.readObject();
        net.setLearningRate(LEARNING_RATE);
        net.setEligDecayRate(ELIG_DECAY_RATE);
        net.setL2Regularization(L2);
        ois.close();
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setLogDebug(boolean b) {
        this.logDebug = true;
    }

    public void setRandomMoveProb(double prob) {
        this.randomMoveProb = prob;
    }

    public void setMakeRandomMoves(boolean b) {
        this.makeRandomMoves = b;
    }

    public boolean getDoDoubleQ() {
        return doDoubleQ;
    }

    public void setDoDoubleQ(boolean doDoubleQ) {
        this.doDoubleQ = doDoubleQ;
    }
}
