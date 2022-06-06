package com.dillard.games.risk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RiskGameState implements GameState<RiskGame> {

    private Map<RiskPlayer, Integer> cardCountByPlayer = new HashMap<>();
    private Map<RiskPlayer, Integer> availableArmiesByPlayer = new HashMap<>();
    private List<Integer> cardRewardList;
    private TerritoryMap<TerritoryState> territoryState = new TerritoryMap<>();
    private TerritoryMapToInt distanceToOpposingAdjacentTerritoryCache = new TerritoryMapToInt();
    private TerritoryMap<List<Territory>[]> adjacentOwnedCache = new TerritoryMap<>();
    private Map<RiskPlayer, Integer> territoryCountCache = null;
    private Map<RiskPlayer, Integer> continentAllowanceCache = null;
    private Map<RiskPlayer, List<TerritoryState>> territoryStatesByOwnerCache = null;
    private boolean currentPlayerHasTakenATerritory = false;
    private int turnNumber = 0;
    private List<String> playerNames;

    public RiskGameState(List<RiskPlayer> players) {
        this.playerNames = Collections.unmodifiableList(
                players.stream().map((player) -> player.getName()).collect(Collectors.toList()));
        initRewards();
        initTerritories();
        initCardCounts(players);
        initAvailableArmies(players);
    }

    private void initCardCounts(List<RiskPlayer> players) {
        for (RiskPlayer player : players) {
            setCardCount(player, 0);
        }
    }

    private void initAvailableArmies(List<RiskPlayer> players) {
        for (RiskPlayer player : players) {
            setAvailableArmies(player, 0);
        }
    }

    private void initTerritories() {
        for (Territory territory : Territory.values()) {
            territoryState.put(territory, new TerritoryState(territory));
        }
    }

    private void initRewards() {
        cardRewardList = new LinkedList<>();
        int reward = 2;
        for (int inc : new int[] {2,3,4,5}) {
            for (int i=0; i<4; i++) {
                cardRewardList.add(reward);
                reward += inc;
            }
        }

        for (int i=0; i<10000; i++) {
            // if this grows too slowly then the game will often end in a stalemate
            cardRewardList.add(reward);
            reward += Math.max(5, reward * 0.1);
        }
    }

    public int getCardCount(RiskPlayer player) {
        return cardCountByPlayer.get(player);
    }

    public void setCardCount(RiskPlayer player, int count) {
        cardCountByPlayer.put(player, count);
    }

    public void incementCardCount(RiskPlayer player) {
        cardCountByPlayer.put(player, cardCountByPlayer.get(player)+1);
    }

    public void removeCards(RiskPlayer currentPlayer, int i) {
        setCardCount(currentPlayer, getCardCount(currentPlayer) - i);
    }

    public void setAvailableArmies(RiskPlayer player, int i) {
        availableArmiesByPlayer.put(player, i);
    }

    public void addAvailableArmies(RiskPlayer player, int i) {
        availableArmiesByPlayer.put(player, availableArmiesByPlayer.get(player) + i);
    }

    public int getAvailableArmies(RiskPlayer player) {
        return availableArmiesByPlayer.get(player);
    }

    public int nextCardReward() {
        return cardRewardList.get(0);
    }

    public void advanceCardReward() {
        cardRewardList.remove(0);
    }

    public int getArmyAllowance(RiskPlayer player) {
        int territoryAllowance = Math.max(3, getTerritoryCount(player) / 3);
        return territoryAllowance + continentAllowance(player);
    }

    private int continentAllowance(RiskPlayer player) {
        //		int sum = 0;
        //		for (Continent continent : Continent.values()) {
        //			if (continent.getTerritoryList().stream().allMatch(t -> territoryState.get(t).getOwner() == player)) {
        //				sum += continent.getReward();
        //			}
        //		}
        //		return sum;
        if (continentAllowanceCache == null) {
            continentAllowanceCache = new HashMap<>();
            for (RiskPlayer p : cardCountByPlayer.keySet()) {
                int sum = 0;
                for (Continent continent : Continent.values()) {
                    if (continent.getTerritoryList().stream().allMatch(t -> territoryState.get(t).getOwner() == p)) {
                        sum += continent.getReward();
                    }
                }
                continentAllowanceCache.put(p, sum);
            }
        }
        return continentAllowanceCache.get(player);
    }

    public final List<TerritoryState> ownedTerritories(RiskPlayer player) {
        if (territoryStatesByOwnerCache == null) {
            territoryStatesByOwnerCache = territoryState.values().stream().collect(
                    Collectors.groupingBy(TerritoryState::getOwner, Collectors.toList()));
        }
        return territoryStatesByOwnerCache.get(player);
        //		List<TerritoryState> l = new ArrayList<>(Territory.VALUES.length);
        //		for (TerritoryState ts : territoryState.values()) {
        //			if (ts.getOwner() == player) {
        //				l.add(ts);
        //			}
        //		}
        //		return l;
        //		return territoryState.values().stream().filter(ts -> ts.getOwner() == player).collect(Collectors.toList());
    }

    public final TerritoryState getTerritoryState(Territory t) {
        return territoryState.get(t);
    }

    public void ownershipChange() {
        invalidateCachedDistanceToOpposingAdjacentTerritory();
        adjacentOwnedCache.clear();
        territoryStatesByOwnerCache = null;
        territoryCountCache = null;
        continentAllowanceCache = null;
    }

    public int getCachedDistanceToOpposingAdjacentTerritory(Territory t) {
        return distanceToOpposingAdjacentTerritoryCache.get(t);
    }

    public void setCachedDistanceToOpposingAdjacentTerritory(Territory t, int dist) {
        distanceToOpposingAdjacentTerritoryCache.put(t, dist);
    }

    public void invalidateCachedDistanceToOpposingAdjacentTerritory() {
        distanceToOpposingAdjacentTerritoryCache.clear();
    }

    @SuppressWarnings("unchecked")
    public final List<Territory> adjacentOwned(TerritoryState ts, boolean isOwned) {
        List<Territory>[] cachedArray = adjacentOwnedCache.get(ts.getTerritory());
        if (cachedArray == null) {
            cachedArray = new List[2];
            adjacentOwnedCache.put(ts.getTerritory(), cachedArray);
        }
        List<Territory> value = cachedArray[isOwned ? 0 : 1];
        if (value == null) {
            value = adjacentOwnedNoCache(ts, isOwned);
            cachedArray[isOwned ? 0 : 1] = value;
        }
        return value;
    }

    private List<Territory> adjacentOwnedNoCache(TerritoryState ts, boolean isOwned) {
        List<Territory> adj = ts.getTerritory().getAdjacentTerritories();
        List<Territory> adjOwned = new ArrayList<>(adj.size());
        RiskPlayer owner = ts.getOwner();
        for (Territory t : adj) {
            if (isOwned == (getTerritoryState(t).getOwner() == owner)) {
                adjOwned.add(t);
            }
        }
        return adjOwned;
    }

    public final int distanceToOpposingAdjacentTerritory(Territory terr) {
        int cachedValue = getCachedDistanceToOpposingAdjacentTerritory(terr);
        if (cachedValue != TerritoryMapToInt.DEFAULT_VALUE) {
            return cachedValue;
        }

        int calculatedValue = distanceToOpposingAdjacentTerritoryNoCache(terr);
        setCachedDistanceToOpposingAdjacentTerritory(terr, calculatedValue);
        return calculatedValue;
    }

    private int distanceToOpposingAdjacentTerritoryNoCache(Territory terr) {
        TerritorySet visited = new TerritorySet();
        TerritorySet previouslyDiscovered = new TerritorySet();
        previouslyDiscovered.add(terr);

        RiskPlayer sourcePlayer = getTerritoryState(terr).getOwner();

        for (int dist = 0; dist<100; dist++) {
            TerritorySet newlyDiscovered = new TerritorySet();
            for (Territory t : previouslyDiscovered.values()) {
                if (!visited.contains(t)) {
                    if (sourcePlayer != getTerritoryState(t).getOwner()) {
                        return dist;
                    }
                    visited.add(t);
                    newlyDiscovered.addAll(t.getAdjacentTerritories());
                }
            }
            previouslyDiscovered = newlyDiscovered;
        }
        throw new RuntimeException();
    }

    public int getTerritoryCount(RiskPlayer player) {
        if (territoryCountCache == null) {
            // Build cached counts for all players
            territoryCountCache = new HashMap<>();
            for (RiskPlayer p : cardCountByPlayer.keySet()) {
                territoryCountCache.put(p, 0);
            }

            for (TerritoryState ts : territoryState.values()) {
                RiskPlayer owner = ts.getOwner();
                territoryCountCache.put(owner, territoryCountCache.get(owner) + 1);
            }
        }
        return territoryCountCache.get(player);
        //		return (int) territoryState.values().stream().filter(ts -> ts.getOwner() == player).count();
    }

    public void resetTurn() {
        currentPlayerHasTakenATerritory = false;
    }

    public void setCurrentPlayerHasTakenATerritory(boolean has) {
        currentPlayerHasTakenATerritory = has;
    }

    public boolean getCurrentPlayerHasTakenATerritory() {
        return currentPlayerHasTakenATerritory;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void incrementTurnNumber() {
        turnNumber++;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    @Override
    public String toString() {
        return new RiskUIConsole().showGameBasic(this);
    }
}
