package com.dillard.games.risk;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import games.ActivationFunctionTanH;
import games.LayeredNNTD;
import games.TDLearningNN;
import games.WeightInitializerGaussianFixedVariance;

public final class NNRiskPlayer extends AbstractRiskPlayer {
    private static final int NUM_FEATURES = 43; // 471;
    private LayeredNNTD net;
    private double lastScore = 0;
    private boolean haveMadeAMove = false;
    private boolean learningMode = false;
//    private DecimalFormat df3 = new DecimalFormat("0.000");

    public NNRiskPlayer(String name) {
        super(name);
        this.net = LayeredNNTD.buildFullyConnected(new int[] {NUM_FEATURES, 10, 1},
                new ActivationFunctionTanH(),
                new WeightInitializerGaussianFixedVariance(1.0/5.0),
                0.1,   // learning rate
                0.9,   // elig decay rate
                0,     // L2 regularization
                0.0001 // L1 regularization
                );
    }

    public NNRiskPlayer(String name, LayeredNNTD net) {
        super(name);
        this.net = net;
    }

    private double[] newFeatureArray() {
        return new double[NUM_FEATURES];
    }

    private double[] createFeatures(RiskGameState state, Move<RiskGame> move) {
        double[] features = newFeatureArray();
        int idx = 0;

        // State features
        // =================
        features[idx++] = state.getArmyAllowance(this);
        features[idx++] = state.getTerritoryCount(this);
        features[idx++] = state.getCurrentPlayerHasTakenATerritory() ? 1 : 0;
        features[idx++] = state.getCardCount(this);
        features[idx++] = state.nextCardReward();

        // TODO
//        for (Territory t : Territory.VALUES) {
//            TerritoryState ts = state.getTerritoryState(t);
//            RiskPlayer owner = ts.getOwner();
//            features[idx++] = owner == this ? 1 : 0;
//            features[idx++] = owner == this ? ts.getArmyCount() : 0;
//            features[idx++] = owner == this ? 0 : ts.getArmyCount();
//        }

        // Move features
        // =================
        if (move instanceof MoveAttack) {
            MoveAttack m = (MoveAttack) move;
            features[idx++] += 1; // indicator

            // indicate from territory
            TerritoryState from = m.getFrom();
//            features[idx + from.getTerritory().ordinal()] = 1;
//            idx += Territory.NUM_TERRITORIES;

            // indicate to territory
            TerritoryState to = m.getTo();
//            features[idx + to.getTerritory().ordinal()] = 1;
//            idx += Territory.NUM_TERRITORIES;

            // from territory army count, to army count
            features[idx++] = from.getArmyCount();
            features[idx++] = to.getArmyCount();

            // army count diff
            features[idx++] = from.getArmyCount() - to.getArmyCount();

            // defending player num territories in continent, num armies in continent, num territories total, num cards
            RiskPlayer defender = to.getOwner();
            List<TerritoryState> defenderOwnedTerritories = state.ownedTerritories(defender);
            int defenderNumTerritoriesInContinent = 0, defenderNumArmiesInContinent = 0, defenderNumTerritoriesTotal = 0;
            for (TerritoryState ts : defenderOwnedTerritories) {
                if (ts.getTerritory().getContinent() == to.getTerritory().getContinent()) {
                    defenderNumTerritoriesInContinent++;
                    defenderNumArmiesInContinent += ts.getArmyCount();
                }
                defenderNumTerritoriesTotal++;
            }
            features[idx++] = defenderNumTerritoriesInContinent;
            features[idx++] = defenderNumArmiesInContinent;
            features[idx++] = defenderNumTerritoriesTotal;
            // defender num cards
            features[idx++] = state.getCardCount(defender);

            // num territories I don't own in continent, num armies not mine in continent, num players owning territories in continent
            int numTerritoriesInContinentNotOwned = 0, numOtherPlayerArmiesInContinent = 0;
            int numMyArmiesInContinent = 0, numMyTerritoriesInContinent = 0;
            Set<String> playerNamesInContinent = new HashSet<>();
            for (Territory tInContinent : to.getTerritory().getContinent().getTerritoryList()) {
                TerritoryState ts = state.getTerritoryState(tInContinent);
                if (ts.getOwner() != this) {
                    playerNamesInContinent.add(ts.getOwner().getName());
                    numTerritoriesInContinentNotOwned++;
                    numOtherPlayerArmiesInContinent += ts.getArmyCount() + 1; // +1 for the "owning" army
                } else {
                    numMyArmiesInContinent += ts.getArmyCount();
                    numMyTerritoriesInContinent++;
                }
            }
            features[idx++] = numTerritoriesInContinentNotOwned;
            features[idx++] = numOtherPlayerArmiesInContinent;
            features[idx++] = playerNamesInContinent.size();

            features[idx++] = numMyArmiesInContinent;
            features[idx++] = numMyTerritoriesInContinent;
            features[idx++] = numMyArmiesInContinent / (1.0 + numMyArmiesInContinent + numOtherPlayerArmiesInContinent);

        } else {
            idx += 14;
        }

        if (move instanceof MovePlaceArmies) {
            MovePlaceArmies m = (MovePlaceArmies) move;
            features[idx++] = m.armiesRemaining;
            features[idx++] = m.territoryArmyCount;
//            features[idx + m.territory.ordinal()] = 1;
//            idx += Territory.NUM_TERRITORIES;

            TerritoryState ts = state.getTerritoryState(m.territory);
            int distance = state.distanceToOpposingAdjacentTerritory(m.territory);
            features[idx++] = distance;

            // Adjacent owned territories/armies
            List<Territory> adjacentOwned = state.adjacentOwned(ts, true);
            int adjacentOwnedTerritories = 0, adjacentOwnedArmies = 0;
            for (Territory aot : adjacentOwned) {
                adjacentOwnedTerritories++;
                TerritoryState aots = state.getTerritoryState(aot);
                adjacentOwnedArmies += aots.getArmyCount();
            }
            features[idx++] = adjacentOwnedTerritories;
            features[idx++] = adjacentOwnedArmies;

            // Adjacent not owned territories/armies, count players
            List<Territory> adjacentNotOwned = state.adjacentOwned(ts, false);
            int adjacentNotOwnedTerritories = 0, adjacentNotOwnedArmies = 0;
            int minAdjacentUnownedArmyCount = Integer.MAX_VALUE;
            Set<String> adjacentPlayerNames = new HashSet<>();
            for (Territory aut : adjacentNotOwned) {
                adjacentNotOwnedTerritories++;
                TerritoryState auts = state.getTerritoryState(aut);
                adjacentNotOwnedArmies += auts.getArmyCount();
                if (auts.getArmyCount() < minAdjacentUnownedArmyCount) {
                    minAdjacentUnownedArmyCount = auts.getArmyCount();
                }
                adjacentPlayerNames.add(auts.getOwner().getName());
            }
            features[idx++] = adjacentNotOwnedTerritories == 0 ? 1 : 0;
            features[idx++] = adjacentNotOwnedTerritories;
            features[idx++] = adjacentNotOwnedArmies;
            features[idx++] = adjacentPlayerNames.size();
            features[idx++] = adjacentNotOwnedTerritories > 0 ? minAdjacentUnownedArmyCount : 0;
            features[idx++] = adjacentNotOwnedTerritories > 0 ? m.territoryArmyCount - minAdjacentUnownedArmyCount : 0;
            features[idx++] = adjacentNotOwnedTerritories > 0 ? m.territoryArmyCount + m.armiesRemaining - minAdjacentUnownedArmyCount : 0;

            // num territories I don't own in continent, num armies not mine in continent, num players owning territories in continent
            int numTerritoriesInContinentNotOwned = 0, numOtherPlayerArmiesInContinent = 0;
            int numMyArmiesInContinent = 0, numMyTerritoriesInContinent = 0;
            Set<String> playerNamesInContinent = new HashSet<>();
            for (Territory tInContinent : ts.getTerritory().getContinent().getTerritoryList()) {
                TerritoryState tsInContinent = state.getTerritoryState(tInContinent);
                if (tsInContinent.getOwner() != this) {
                    playerNamesInContinent.add(tsInContinent.getOwner().getName());
                    numTerritoriesInContinentNotOwned++;
                    numOtherPlayerArmiesInContinent += tsInContinent.getArmyCount();
                } else {
                    numMyArmiesInContinent += ts.getArmyCount();
                    numMyTerritoriesInContinent++;
                }
            }
            features[idx++] = numTerritoriesInContinentNotOwned;
            features[idx++] = numOtherPlayerArmiesInContinent;
            features[idx++] = playerNamesInContinent.size();

            features[idx++] = numMyArmiesInContinent;
            features[idx++] = numMyTerritoriesInContinent;
            features[idx++] = numMyArmiesInContinent / (1.0 + numMyArmiesInContinent + numOtherPlayerArmiesInContinent);

            int countAdjacentOpposingArmies = countOpposingAdjacentArmies(m.territory, state)[1];
            features[idx++] = countAdjacentOpposingArmies;
            features[idx++] = m.territoryArmyCount / (1.0 + m.territoryArmyCount + countAdjacentOpposingArmies);

        } else {
            idx += 12;
        }

        if (move instanceof MoveUseCards) {
            features[idx++] += 1; // indicator
        } else {
            idx += 1;
        }

        if (move instanceof MoveRegroup) {
            MoveRegroup m = (MoveRegroup) move;
            features[idx++] += 1; // indicator

            // indicate from territory
            TerritoryState from = m.getFrom();
//            features[idx + from.getTerritory().ordinal()] = 1;
//            idx += Territory.NUM_TERRITORIES;

            // indicate to territory
            TerritoryState to = m.getTo();
//            features[idx + to.getTerritory().ordinal()] = 1;
//            idx += Territory.NUM_TERRITORIES;

            int countToAdjacentOpposingArmies = countOpposingAdjacentArmies(to.getTerritory(), state)[1];
            features[idx++] = countToAdjacentOpposingArmies;
            features[idx++] = m.getTo().getArmyCount() / (1.0 + countToAdjacentOpposingArmies + m.getTo().getArmyCount());

            int countFromAdjacentOpposingArmies = countOpposingAdjacentArmies(from.getTerritory(), state)[1];
            features[idx++] = countFromAdjacentOpposingArmies;
            features[idx++] = m.getFrom().getArmyCount() / (1.0 + countFromAdjacentOpposingArmies + m.getFrom().getArmyCount());

            // TODO might want more features, like about how good of a territory it is to attack from / defend from
        } else {
            idx += 5;
        }

        if (move instanceof MoveAdvance) {
            MoveAdvance m = (MoveAdvance) move;
            features[idx++] += 1; // indicator

            // indicate from territory
            TerritoryState from = m.getFrom();
//            features[idx + from.getTerritory().ordinal()] = 1;
//            idx += Territory.NUM_TERRITORIES;

            // indicate to territory
            TerritoryState to = m.getTo();
//            features[idx + to.getTerritory().ordinal()] = 1;
//            idx += Territory.NUM_TERRITORIES;

            int countToAdjacentOpposingArmies = countOpposingAdjacentArmies(to.getTerritory(), state)[1];
            features[idx++] = countToAdjacentOpposingArmies;
            features[idx++] = m.getTo().getArmyCount() / (1.0 + countToAdjacentOpposingArmies + m.getTo().getArmyCount());

            int countFromAdjacentOpposingArmies = countOpposingAdjacentArmies(from.getTerritory(), state)[1];
            features[idx++] = countFromAdjacentOpposingArmies;
            features[idx++] = m.getFrom().getArmyCount() / (1.0 + countFromAdjacentOpposingArmies + m.getFrom().getArmyCount());

        } else {
            idx += 5;
        }

        if (move instanceof MoveNull) {
            // NULL MOVE
            features[idx++] = 1; // indicator
        } else {
            idx += 1;
        }

        return features;
    }

//	private double[] lastMoveFeatures;

    @Override
    public Move<RiskGame> getMove(
            RiskGameState gameState,
            List<Move<RiskGame>> moves) {

        // Get the best move
        Move<RiskGame> move = super.getMove(gameState, moves);

        if (learningMode) {
            // Re-score the best move to get the activation levels for setting eligibility traces,
            // and to get the score for the current move
            double currentScore = scoreMove(move, gameState);

            if (haveMadeAMove) {
                // learn on the difference between the current move's score and the last move's score,
                // if there was a last move
                net.tdLearn(new double[] {currentScore - lastScore});

                // TODO remove, just for debugging
//				double lastScoreAfterLearn = score(lastMoveFeatures);
//				double scoreDiff = lastScoreAfterLearn - lastScore;
//				System.out.println("Error :" + (currentScore - lastScore) +
//				        " Score diff: " + scoreDiff +
//				        " Last score " + lastScore +
//				        " Last score after learn " + lastScoreAfterLearn +
//				        " currentScore " + currentScore
//				        );
            }
            //			lastMoveFeatures = createFeatures(gameState, move);

            // save the current score for next time
            lastScore = currentScore;

            // update eligibility traces for this move
            net.updateElig();

            haveMadeAMove = true;
        }

        return move;
    }

    public void learnFinalResult(boolean isWinner, boolean isDraw, RiskGameState state) {
        double score = isWinner ? 1.0 : -1.0;
        if (isDraw) {
            // range between -0.5 and 0.5, based on proportion of territory
            score = (state.getTerritoryCount(this) / (double) Territory.NUM_TERRITORIES) - 0.5;
        }
        net.tdLearn(new double[] {score - lastScore});
//        System.out.println(String.format("Final score: %s", df3.format(score)));
    }

    public void resetLearning() {
        net.reset(); // reset eligibility traces
    }

    @Override
    protected double scoreMove(Move<RiskGame> move, RiskGameState state) {
        double score = score(createFeatures(state, move));
        //		System.out.println(score);
        return score;
    }


    private double score(double[] features) {
        return net.activate(features)[0];
    }

    public TDLearningNN getNeuralNet() {
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
        ois.close();
    }
}
