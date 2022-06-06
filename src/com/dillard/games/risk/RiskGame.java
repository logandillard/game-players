package com.dillard.games.risk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RiskGame implements Game {

    public static final int CARDS_FOR_TRADE_IN = 3;
    public static final int CARDS_FOR_MANDATORY_TRADE_IN = 6;
    public static final String THE_CAT_PLAYER_NAME = "The Cat";
    public static final int MAXIMUM_NUMBER_OF_TURNS = 100;

    private Random random = new Random(37824598);
    private List<RiskPlayer> players;
    private int currentPlayerIndex = 0;
    private RiskGameState gameState;
    private boolean logChanges = false;

    public RiskGame(List<RiskPlayer> thePlayers, long randomSeed) {
        validateNames(thePlayers);
        this.players = new ArrayList<>(thePlayers);
        gameState = new RiskGameState(players);
        random = new Random(randomSeed);
        initalizeGame();
    }

    private void validateNames(List<RiskPlayer> players) {
        Set<String> names = new HashSet<>();
        for (RiskPlayer player : players) {
            if (names.contains(player.getName())) {
                throw new RuntimeException("No duplicate player names allowed. Saw more than one of: " + player.getName());
            }
            names.add(player.getName());
        }
    }

    private void initalizeGame() {
        // distribute territories randomly
        int playerIdx = 0;
        for (Territory t : Territory.values()) {
            gameState.getTerritoryState(t).setArmyCount(1);
            gameState.getTerritoryState(t).setOwner(players.get(playerIdx));
            playerIdx = (playerIdx+1) % players.size();
        }

        // distribute armies randomly
        final int startingArmies = 80 / players.size();
        for (RiskPlayer player : players) {
            List<TerritoryState> ts = gameState.ownedTerritories(player);
            for (int i=0; i<startingArmies; i++) {
                TerritoryState t = ts.get(random.nextInt(ts.size()));
                t.setArmyCount(t.getArmyCount() + 1);
            }
        }
    }

    public void executeCurrentTurn() {
        gameState.resetTurn();

        if (gameState.getTerritoryCount(currentPlayer()) > 0) {
            for (TurnPhase phase : TurnPhase.values()) {
                if (isTerminated()) {
                    break;
                }
                executePhase(phase, players.get(currentPlayerIndex));
            }
        }

        currentPlayerIndex = (currentPlayerIndex+1) % players.size();
    }

    private void executePhase(TurnPhase phase, RiskPlayer player) {
        int initialTerritoryCount = gameState.getTerritoryCount(player);
        if (initialTerritoryCount == 0) {
            return;
        }

        Move<RiskGame> chosenMove = null;

        switch (phase) {
        case USE_CARDS:
            executeUseCardsPhase(player);
            break;

        case PLACE_ARMIES:
            executePlaceArmiesPhase(player);
            break;

        case ATTACK:
            executeAttackPhase(player, chosenMove);
            break;

        case REGROUP:
            executeRegroupPhase(player);
            break;

        default: throw new IllegalStateException("Unknown phase: " + phase);
        }
    }
    
    // TODO remove, just a performance test
    private boolean skipRegroup = false;

    private void executeRegroupPhase(RiskPlayer player) {
        if (skipRegroup) {
            return;
        }
        
        Move<RiskGame> chosenMove;
        Set<TerritoryPair> usedMoves = new HashSet<>();
        TerritoryMapToInt remainingArmyMoveCounts = new TerritoryMapToInt();
        for (Territory t : Territory.VALUES) {
            TerritoryState ts = gameState.getTerritoryState(t);
            remainingArmyMoveCounts.put(t, ts.getArmyCount() + 1);
        }
        do {
            // a player can only move out of a country the number of armies
            // that started the regroup phase in that country
            // this will be a big speed improvement in the game
            List<Move<RiskGame>> moves = new ArrayList<>();
            moves.add(new MoveNull());
            moves.addAll(regroupMoves(player, usedMoves, remainingArmyMoveCounts));
            chosenMove = player.getMove(gameState, moves);

            if (chosenMove instanceof MoveRegroup) {
                // if armyAllowance is huge, we place more than one at a time for performance
                MoveRegroup mr = (MoveRegroup)chosenMove;
                int n = batchMove(mr.getFrom().getArmyCount());
                for (int i=0; i<n; i++) {
                    applyMove(chosenMove);
                    Territory from = mr.getFrom().getTerritory();
                    remainingArmyMoveCounts.put(from, remainingArmyMoveCounts.get(from)-1);
                }
                usedMoves.add(new TerritoryPair(mr.getFrom().getTerritory(), mr.getTo().getTerritory()));
                
            } else {
                applyMove(chosenMove);
            }

        } while (!(chosenMove instanceof MoveNull));
    }

    private void executeAttackPhase(RiskPlayer player, Move<RiskGame> chosenMove) {
        do {
            List<Move<RiskGame>> moves = attackMoves(player);
            if (moves.isEmpty()) break;

            moves.add(new MoveNull());
            chosenMove = player.getMove(gameState, moves);
            
            if (chosenMove instanceof MoveAttack) {
                MoveAttack ma = (MoveAttack) chosenMove;
                // if FROM has a huge number of armies, attack more than once for performance
                int n = Math.max(1, 
                            Math.min(batchMove(ma.getFrom().getArmyCount()), 
                                ma.getTo().getArmyCount())
                        );
                for (int i=0; i<n; i++) {
                    applyMove(chosenMove);
                }
            } else {
                applyMove(chosenMove);
            }
            
        } while (!(chosenMove instanceof MoveNull));

        if (gameState.getCurrentPlayerHasTakenATerritory()) {
            gameState.incementCardCount(player);
        }
    }
    
    private void executePostConquerAdvance(TerritoryState from, TerritoryState to) {
        Move<RiskGame> chosenMove = null;
        RiskPlayer player = currentPlayer();

        do {
            if (from.getArmyCount() == 0) {
                break;
            }

            List<Move<RiskGame>> moves = new ArrayList<>();
            moves.add(new MoveNull());
            moves.add(new MoveAdvance(from, to, from.getArmyCount()));
            chosenMove = player.getMove(gameState, moves);

            // if armyAllowance is huge, we place more than one at a time for performance
            int n = batchMove(from.getArmyCount());
            for (int i=0; i<n; i++) {
                applyMove(chosenMove);
            }
        } while (!(chosenMove instanceof MoveNull));
    }

    private void executePlaceArmiesPhase(RiskPlayer player) {
        int armyAllowance = gameState.getArmyAllowance(player) + gameState.getAvailableArmies(player);
        gameState.setAvailableArmies(player, 0);
        while (armyAllowance > 0) {
            List<Move<RiskGame>> moves = placeArmyMoves(player, armyAllowance);
            Move<RiskGame> chosenMove = player.getMove(gameState, moves);

            // if armyAllowance is huge, we place more than one at a time for performance
            int numArmiesRemaining = ((MovePlaceArmies) chosenMove).getArmiesRemaining();
            int n = batchMove(numArmiesRemaining);

            for (int i=0; i<n; i++) {
                applyMove(chosenMove);
                armyAllowance--;
            }
        }
    }

    private void executeUseCardsPhase(RiskPlayer player) {
        if (playerCanTradeInCards(player)) {
            List<Move<RiskGame>> moves = new ArrayList<>();
            if (!playerMustTradeInCards(player)) {
                moves.add(new MoveNull());
            }
            moves.add(new MoveUseCards(gameState.nextCardReward(), gameState.getCardCount(player)));
            applyMove(currentPlayer().getMove(gameState, moves));
        }
    }

    private Collection<? extends Move<RiskGame>> regroupMoves(
            RiskPlayer player, Set<TerritoryPair> usedMoves, TerritoryMapToInt remainingArmyMoveCounts) {
        List<Move<RiskGame>> moves = new ArrayList<>();
        List<TerritoryState> owned = gameState.ownedTerritories(player);
        // regroup just to adjacent territories
        for (TerritoryState ts : owned) {
            if (ts.getArmyCount() < 1 || remainingArmyMoveCounts.get(ts.getTerritory()) < 1) continue;

            for (Territory adjacentOwned : gameState.adjacentOwned(ts, true)) {
                // if we haven't moved in the opposite direction this turn
                if (!usedMoves.contains(new TerritoryPair(adjacentOwned, ts.getTerritory()))) {
                    moves.add(new MoveRegroup(ts, gameState.getTerritoryState(adjacentOwned), ts.getArmyCount()));
                }
            }
        }
        return moves;
    }

    private List<Move<RiskGame>> attackMoves(RiskPlayer player) {
        List<Move<RiskGame>> moves = new ArrayList<>();
        List<TerritoryState> owned = gameState.ownedTerritories(player);
        for (TerritoryState ts : owned) {
            for (Territory adjacentNonOwned : gameState.adjacentOwned(ts, false)) {
                if (ts.getArmyCount() > 0) {
                    moves.add(new MoveAttack(ts, gameState.getTerritoryState(adjacentNonOwned)));
                }
            }
        }
        return moves;
    }

    private List<Move<RiskGame>> placeArmyMoves(RiskPlayer player, int armiesRemaining) {
        List<Move<RiskGame>> moves = new ArrayList<>();
        for (TerritoryState ts : gameState.ownedTerritories(player)) {
            moves.add(new MovePlaceArmies(ts.getTerritory(), ts.getArmyCount(), armiesRemaining));
        }
        return moves;
    }

    void applyMove(MoveAttack move) {

        if (move.getFrom().getArmyCount() < 1) {
            return;
        }

        final int numAttacks = Math.min(3, move.getFrom().getArmyCount());
        int[] attackRolls = new int[numAttacks];
        for (int i=0; i<attackRolls.length; i++) {
            int roll = random.nextInt(6);
            attackRolls[i] = -roll; // negative so that the sort is "descending"
        }
        Arrays.sort(attackRolls);

        final int numDefends = Math.min(2, move.getTo().getArmyCount() + 1);
        int[] defendRolls = new int[numDefends];
        for (int i=0; i<defendRolls.length; i++) {
            int roll = random.nextInt(6);
            defendRolls[i] = -roll; // negative so that the sort is "descending"
        }
        Arrays.sort(defendRolls);

        int attackLosses = 0;
        int defendLosses = 0;
        for (int i=0; i<Math.min(numAttacks, numDefends); i++) {
            if (-attackRolls[i] > -defendRolls[i]) { // negative to reverse the negative above
                defendLosses++;
            } else {
                attackLosses++;
            }
        }

        if (logChanges) {
            System.out.println(move.getFrom().getOwner().getName() +
                    " " + move.getFrom().getTerritory() +
                    "(" + move.getFrom().getArmyCount() + ")" +
                    " -" + attackLosses +
                    " attacks " +
                    move.getTo().getOwner().getName() +
                    " " + move.getTo().getTerritory() +
                    "(" + move.getTo().getArmyCount() + ")" +
                    " -" + defendLosses
                    );
        }

        move.getFrom().addArmies(-attackLosses);
        move.getTo().addArmies(-defendLosses);

        if (move.getTo().getArmyCount() < 0) {
            RiskPlayer oldOwner = move.getTo().getOwner();
            move.getTo().setOwner(currentPlayer());
            move.getTo().setArmyCount(0);
            // taking over a country costs one army
            move.getFrom().addArmies(-1);

            ownershipChange();

            // the game state needs to specify that this player has taken a territory this turn
            gameState.setCurrentPlayerHasTakenATerritory(true);

            // now they can transfer armies here
            executePostConquerAdvance(move.getFrom(), move.getTo());

            // if this player is gone from the game, the conquering player gets his cards
            if (gameState.getTerritoryCount(oldOwner) == 0) {
                gameState.setCardCount(currentPlayer(),
                        gameState.getCardCount(currentPlayer()) + gameState.getCardCount(oldOwner));
                gameState.setCardCount(oldOwner, 0);

                //				// remove the player from our list, reset the currentPlayerIndex
                //				RiskPlayer currentPlayer = currentPlayer();
                //				players.remove(oldOwner);
                //				currentPlayerIndex = players.indexOf(currentPlayer);
            }
        }
    }

    private void ownershipChange() {
        gameState.ownershipChange();
    }

    void applyMove(MoveRegroup move) {
        RiskPlayer currentPlayer = currentPlayer();
        TerritoryState fromState = move.getFrom();
        TerritoryState toState = move.getTo();

        if (currentPlayer != fromState.getOwner() ||
                currentPlayer != toState.getOwner() ||
                !fromState.getTerritory().isAdjacent(toState.getTerritory()) ||
                fromState.getArmyCount() < 1) {
            throw new IllegalStateException("Invalid move: " + move);
        }

        move.getFrom().addArmies(-1);
        move.getTo().addArmies(1);

        if (logChanges) {
            System.out.println(move.getFrom().getOwner().getName() +
                    " " + move.getFrom().getTerritory() +
                    " regroups to " +
                    move.getTo().getTerritory()
                    );
        }
    }

    void applyMove(MoveAdvance move) {
        RiskPlayer currentPlayer = currentPlayer();
        TerritoryState fromState = move.getFrom();
        TerritoryState toState = move.getTo();

        if (currentPlayer != fromState.getOwner() ||
                currentPlayer != toState.getOwner() ||
                !fromState.getTerritory().isAdjacent(toState.getTerritory()) ||
                fromState.getArmyCount() < 1) {
            throw new IllegalStateException("Invalid move: " + move);
        }

        move.getFrom().addArmies(-1);
        move.getTo().addArmies(1);

        if (logChanges) {
            System.out.println(move.getFrom().getOwner().getName() +
                    " " + move.getFrom().getTerritory() +
                    " advances to " +
                    move.getTo().getTerritory()
                    );
        }
    }

    void applyMove(MoveUseCards move) {
        RiskPlayer currentPlayer = currentPlayer();

        // validate
        validatePlayerCanTradeInCards(currentPlayer);

        gameState.removeCards(currentPlayer, CARDS_FOR_TRADE_IN);

        gameState.advanceCardReward();

        gameState.addAvailableArmies(currentPlayer, move.currentReward);

        if (logChanges) {
            System.out.println(currentPlayer.getName() + " trades in cards for " + move.currentReward);
        }
    }

    private void validatePlayerCanTradeInCards(RiskPlayer player) {
        // TODO cards are simple. they're all the same. you don't have to have three of a kind or one of each
        if (gameState.getCardCount(player) < CARDS_FOR_TRADE_IN) {
            throw new IllegalStateException("Player does not have enough cards: " +
                    gameState.getCardCount(player) + " " + player);
        }
    }

    private boolean playerMustTradeInCards(RiskPlayer player) {
        return gameState.getCardCount(player) >= CARDS_FOR_MANDATORY_TRADE_IN;
    }

    private boolean playerCanTradeInCards(RiskPlayer player) {
        // TODO cards are simple. they're all the same. you don't have to have three of a kind or one of each
        return gameState.getCardCount(player) >= CARDS_FOR_TRADE_IN;
    }

    void applyMove(MovePlaceArmies move) {

        // validate
        TerritoryState territoryState = gameState.getTerritoryState(move.territory);
        if (territoryState.getOwner() != currentPlayer()) {
            throw new IllegalStateException("Illegal move: " + move + " territory is not owned by current player " +
                    territoryState);
        }

        territoryState.addArmies(1);
    }

    void applyMove(MoveNull move) {
    }

    void applyMove(Move<RiskGame> move) {
        move.execute(this);
    }

    private int batchMove(int size) {
        int n = 1;
        if (size > 1000) {
            n = 200;
        } else if (size > 500) {
            n = 100;
        } else if (size > 250) {
            n = 50;
        } else if (size > 100) {
            n = 20;
        } else if (size > 50) {
            n = 10;
        } else if (size > 25) {
            n = 5;
        } else if (size > 10) {
            n = 2;
        }
        return n;
    }

    private RiskPlayer currentPlayer() {
        return players.get(currentPlayerIndex);
    }

    @Override
    public void play() {
        while (!isTerminated()) {
            executeCurrentTurn();
            gameState.incrementTurnNumber();

//            // TODO remove here for debugging
//            if (gameState.getTurnNumber() == 600) {
//                new RiskUIConsole().showGame(gameState);
//                this.setLogChanges(true);
//            }
//            if (gameState.getTurnNumber() == 610) {
//                new RiskUIConsole().showGame(gameState);
//                throw new RuntimeException("Game went on too long");
//            }
        }
    }

    @Override
    public boolean isTerminated() {
        return gameState.getTerritoryCount(gameState.getTerritoryState(Territory.ALASKA).getOwner()) == Territory.values().length
                || isStalemate();
    }

    private boolean isStalemate() {
        return gameState.getTurnNumber() >= MAXIMUM_NUMBER_OF_TURNS;
    }

    @Override
    public String getWinner() {
        if (!isTerminated()) {
            throw new IllegalStateException();
        }

        if (isStalemate()) {
            return THE_CAT_PLAYER_NAME;
        }

        return gameState.getTerritoryState(Territory.ALASKA).getOwner().getName();
    }

    public void setLogChanges(boolean doLog) {
        logChanges = doLog;
    }

    public RiskGameState getState() {
        return gameState;
    }
}
