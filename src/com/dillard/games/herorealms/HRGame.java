package com.dillard.games.herorealms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class HRGame {

    public static final int MAXIMUM_NUMBER_OF_TURNS = 100;

    private Random random;
    private List<HRPlayer> players;
    private HRGameState gameState;
    private boolean logChanges = false;

    public HRGame(List<HRPlayer> thePlayers, long randomSeed) {
        validateNames(thePlayers);
        this.players = new ArrayList<>(thePlayers);
        gameState = new HRGameState(players, randomSeed);
        random = new Random(randomSeed);
        initalizeGame();
    }

    private void initalizeGame() {
    }

    public void executeCurrentTurn() {
        if (logChanges) System.out.println("\n\nStarting turn for " + gameState.currentPlayer().getName());
        gameState.startTurn();
        gameState.drawMarket(gameState.currentPlayer());
        if (logChanges) System.out.println("Market: " + gameState.getMarket());

        int cardsToDraw = gameState.getTurnNumber() == 0 ? 3 : 5;

        List<Card> hand = gameState.drawCards(gameState.currentPlayer(), cardsToDraw);
        List<Card> champions = new ArrayList<>(gameState.getChampions(gameState.currentPlayer()));

        if (logChanges) {
            System.out.println("Champions: " + champions.toString());
            System.out.println("Hand: " + hand.toString());
        }

        hand = discardCards(hand, gameState.discardCardCount());
        gameState.clearDiscardCardCount();

        List<CardWrapper> cardWrappers = new ArrayList<>();
        for (Card card : hand) {
            cardWrappers.add(new CardWrapper(card));
        }
        for (Card card : champions) {
            cardWrappers.add(new CardWrapper(card, true));
        }
        boolean didDrawThenDiscard = false;
        do {
            // draw+discard might lead to more cards to draw, so we do this in a loop
            drawBonusCards(cardWrappers);

            int cardsDrawn = drawThenDiscard(cardWrappers);
            didDrawThenDiscard = cardsDrawn > 0;
        } while (didDrawThenDiscard);

        List<Card> cardsInPlay = CardWrapper.toCards(cardWrappers);
        hand = null;

        if (logChanges) {
            System.out.println("Cards in play: " + cardsInPlay.toString());
        }

        champions = filterChampions(cardsInPlay, true);
        gameState.setChampions(gameState.currentPlayer(), champions);

        TurnSummary turnSummary = TurnSummary.calculateTurnSummary(cardsInPlay);

        // prepareChampions
        if (turnSummary.prepareChampions > 0) {
            if (!champions.isEmpty()) {
                Collections.sort(champions, new Cards.CostComparator());
                Card topChamp = champions.get(champions.size() - 1);
                for (int i=0; i<turnSummary.prepareChampions; i++) {
                    turnSummary = turnSummary.plusPreparedChampion(topChamp, cardsInPlay);
                }
            }
        }

        stunChampions(turnSummary);

        if (turnSummary.opponentDiscard > 0) {
            if (logChanges) System.out.println("Opponent will discard " + turnSummary.opponentDiscard);
            gameState.setDiscardCardCount(turnSummary.opponentDiscard);
        }

        int remainingDamage = applyDamageIncludingSacrificing(cardsInPlay, turnSummary.damage);

        if (isTerminated()) {
            return;
        }

        // TODO use option gold if desired
        buyCards(turnSummary);

        // TODO use option health
        gainHealth(turnSummary);

        sacForDamage(cardsInPlay, remainingDamage);

        sacrifice(turnSummary, cardsInPlay);

        moveFromDiscardToDeck(turnSummary);

        champions = filterChampions(cardsInPlay, true);
        gameState.setChampions(gameState.currentPlayer(), champions);

        List<Card> nonChampions = filterChampions(cardsInPlay, false);
        gameState.discard(gameState.currentPlayer(), nonChampions);

        gameState.endTurn();
    }

    @SafeVarargs
    private List<Card> cat(List<Card>... lists) {
        List<Card> l = new ArrayList<>();
        for (List<Card> list : lists) {
            l.addAll(list);
        }
        return l;
    }

//    private void checkDupes(List<Card> cards) {
//        for (int i=0; i<cards.size(); i++) {
//            if (cards.get(i).name == "gold" || cards.get(i).name == "firegem") continue;
//            for (int j=i+1; j<cards.size(); j++) {
//                if (cards.get(i) == cards.get(j)) {
//                    System.out.println(cards);
//                    System.out.println(cards.get(i));
//                    System.out.println(cards.get(j));
//                    throw new RuntimeException();
//                }
//            }
//        }
//    }

    private List<Card> filterChampions(List<Card> cards, boolean isChampion) {
        return cards.stream().filter((c) -> c.isChampion == isChampion).collect(Collectors.toList());
    }

    private int drawBonusCards(List<CardWrapper> cardWrappers) {
        int cardsAlreadyDrawn = 0;
        int[] colorCount = Cards.colorCounts(CardWrapper.toCards(cardWrappers));
        for (int i=0; i<cardWrappers.size(); i++) {
            CardWrapper w = cardWrappers.get(i);
            if (w.hasCheckedForDraw) {
                continue;
            }
            boolean synergy = colorCount[w.card.color.ordinal()] > 1;
            int drawCount = w.card.cardsToDraw(synergy);
            if (drawCount > 0) {
                // do we also need to move a synergy card?
                boolean needsSynergy = w.card.cardsToDraw(false) < drawCount;
                if (needsSynergy) {
                    findOrPlayHighestCostSynergyCard(w.card, cardWrappers);
                }
                w.hasPlayed = true;

                // Draw cards
                List<Card> newCards = gameState.drawCards(gameState.currentPlayer(), drawCount);
                for (Card c: newCards) cardWrappers.add(new CardWrapper(c));

                // Record keeping
                if (logChanges) System.out.println("Drawing " +drawCount+ " extra card from " + w.card);
                cardsAlreadyDrawn++;
            }
            w.hasCheckedForDraw = true;
        }

        return cardsAlreadyDrawn;
    }

    private int drawThenDiscard(List<CardWrapper> cardWrappers) {
        int cardsDrawn = 0;
        int[] colorCount = Cards.colorCounts(CardWrapper.toCards(cardWrappers));
        // draw-then-discard one at a time
        for (int i=0; i<cardWrappers.size(); i++) {
            CardWrapper w = cardWrappers.get(i);
            if (w.hasCheckedForDrawDiscard) {
                continue;
            }

            boolean synergy = colorCount[w.card.color.ordinal()] > 1;
            int countDrawThenDiscard = w.card.drawThenDiscard(synergy);
            if (countDrawThenDiscard > 0) {
                // do we also need to move a synergy card?
                boolean needsSynergy = w.card.drawThenDiscard(false) < countDrawThenDiscard;
                if (needsSynergy) {
                    findOrPlayHighestCostSynergyCard(w.card, cardWrappers);
                }
                w.hasPlayed = true;

                // finally actually draw then discard
                for (int j=0; j<countDrawThenDiscard; j++) {
                    if (logChanges) System.out.println("Drawing 1 extra card from " + w.card);

                    // Draw
                    List<Card> newCards = gameState.drawCards(gameState.currentPlayer(), 1);
                    for (Card c: newCards) cardWrappers.add(new CardWrapper(c));

                    // Discard
                    List<CardWrapper> unplayed = cardWrappers.stream().filter(wr -> !wr.hasPlayed).collect(Collectors.toList());
                    discardCardWrappers(unplayed, 1);
                    cardsDrawn++;
                }

                // start our loop over because we moved a card, drew a card, and discarded a card
                // so anything could have changed, but we moved anything we used, so it's safe
                i = 0;
            }
            w.hasCheckedForDrawDiscard = true;
        }
        return cardsDrawn;
    }

    private void findOrPlayHighestCostSynergyCard(Card card, List<CardWrapper> cardWrappers) {
        for (CardWrapper w : cardWrappers) {
            if (w.card == card) {
                continue;
            }
            if (w.card.color == card.color && w.hasPlayed) {
                return;
            }
        }

        Optional<CardWrapper> op = cardWrappers.stream()
                .filter((w) -> w.card != card && w.card.color == card.color)
                .max(new Cards.WrapperCostComparator());
        if (!op.isPresent()) {
            System.out.println(card);
            System.out.println(cardWrappers);
        }
        CardWrapper synergyW = op.get();
        synergyW.hasPlayed = true;
    }

    private Card findAndMovePlayedOrHighestCostSynergyCard(Card card, List<Card> playedCards, List<Card> hand) {
        Optional<Card> played = playedCards.stream()
                .filter(c -> (c.color == card.color))
                .max(new Cards.CostComparator());
        if (played.isPresent()) {
            return played.get();
        }

        Optional<Card> op = hand.stream()
                .filter((c) -> c != card && c.color == card.color)
                .max(new Cards.CostComparator());
        if (!op.isPresent()) {
            System.out.println(card);
            System.out.println(playedCards);
            System.out.println(hand);
        }
        Card synergyCard = op.get();

        moveToPlayed(synergyCard, playedCards, hand);

        return synergyCard;
    }

    private void moveToPlayed(Card c, List<Card> playedCards, List<Card> hand) {
        playedCards.add(c);
        hand.remove(c);
    }

    private void stunChampions(TurnSummary turnSummary) {
        HRPlayer opponent = gameState.currentOpponent();
        for (int i=0; i<turnSummary.stunChampions; i++) {
            List<Card> opponentChampions = gameState.getChampions(opponent);
            if (opponentChampions.isEmpty()) {
                return;
            }
            Collections.sort(opponentChampions, new Cards.ChampionToStunComparator());
            Card championToStun = opponentChampions.get(0);
            if (championToStun == null) break;
            if (logChanges) System.out.println("Stunning champion " + championToStun);
            gameState.stunChampion(opponent, championToStun);
        }
    }

    private void sacrifice(TurnSummary turnSummary, List<Card> cardsInPlay) {
        // include sacrifice effects from red cards,
        // but not sac'ing firegems/other cards with a sacrifice choice
        // you can sacrifice cards in play that you have already used!
        int numSacrificed = 0;
        HRPlayer player = gameState.currentPlayer();

        while (numSacrificed < turnSummary.sacrifice) {
            List<Card> nonChamps = cardsInPlay.stream().filter(c -> !c.isChampion).collect(Collectors.toList());
            List<Card> discardPile = gameState.getDiscardPile(gameState.currentPlayer());
            List<Card> sacPool = new ArrayList<>();
            sacPool.addAll(discardPile);
            sacPool.addAll(nonChamps);
            if (sacPool.isEmpty()) {
                return;
            }
            Card toSac = player.chooseSacrifice(sacPool, gameState);
            if (toSac == null) {
                return;
            }
            if (logChanges) System.out.println("Sacrificing " + toSac);
            if (!discardPile.remove(toSac)) {
                cardsInPlay.remove(toSac);
            }
            gameState.addPlayerSacrificed(player, toSac);
            numSacrificed++;
        }
    }

    private void moveFromDiscardToDeck(TurnSummary turnSummary) {
        HRPlayer currentPlayer = gameState.currentPlayer();

        int numChampMoved = 0;
        while (numChampMoved < turnSummary.moveChampionFromDiscardToDeck) {
            List<Card> discardPile = gameState.getDiscardPile(currentPlayer);
            List<Card> discardPileChamps = discardPile.stream().filter((c) -> c.isChampion).collect(Collectors.toList());
            if (discardPileChamps.isEmpty()) {
                break;
            }
            Collections.sort(discardPileChamps, new Cards.CostComparator());
            Card card = discardPileChamps.remove(discardPileChamps.size() - 1);
            discardPile.remove(card);
            if (logChanges) System.out.println("Moving champ from discard pile to deck: " + card);
            gameState.putOnTopOfLibrary(currentPlayer, card);
            numChampMoved++;
        }

        int numMoved = 0;
        while (numMoved < turnSummary.moveAnyFromDiscardToDeck) {
            List<Card> discardPile = gameState.getDiscardPile(currentPlayer);
            if (discardPile.isEmpty()) {
                break;
            }
            Collections.sort(discardPile, new Cards.CostComparator());
            if (discardPile.get(discardPile.size() - 1).cost == 0) {
                break;
            }
            Card card = discardPile.remove(discardPile.size()-1);
            if (logChanges) System.out.println("Moving any from discard pile to deck: " + card);
            gameState.putOnTopOfLibrary(currentPlayer, card);
            numMoved++;
        }
    }

    private void buyCards(TurnSummary turnSummary) {
        HRPlayer player = gameState.currentPlayer();
        // TODO include option gold
        int goldRemaining = turnSummary.gold;
        List<Card> market = gameState.getMarket();
        List<Card> cardsToBuy = new ArrayList<>(market);
        // Always include the option of a firegem
        cardsToBuy.add(Cards.FIREGEM);

        boolean didBuy = false;
        int buysPutOnTop = 0, buysPutInHand = 0, actionsPutOnTop = 0;
        do {
            // Filter to what is affordable
            final int currentGoldRemaining = goldRemaining;
            cardsToBuy = cardsToBuy.stream().filter((c) -> c.cost <= currentGoldRemaining).collect(Collectors.toList());

            if (cardsToBuy.isEmpty()) {
                return;
            }

            // Ask the player
            Card cardToBuy = player.chooseBuy(cardsToBuy, gameState, goldRemaining);

            // buy the card
            if (cardToBuy != null) {
                if (logChanges) System.out.println("Buying card " + cardToBuy);
                // check where to put this card
                if (turnSummary.nextBuyInHand > buysPutInHand) {
//                    System.out.println("Sorry, this is going on top");
                    gameState.putOnTopOfLibrary(player, cardToBuy);
                    buysPutInHand++;
                } else if (turnSummary.nextActionOnTop > actionsPutOnTop && cardToBuy.isAction()) {
                    gameState.putOnTopOfLibrary(player, cardToBuy);
                    actionsPutOnTop++;
                } else if (turnSummary.nextBuyOnTop > buysPutOnTop) {
                    gameState.putOnTopOfLibrary(player, cardToBuy);
                    buysPutOnTop++;
                } else {
                    gameState.getDiscardPile(player).add(cardToBuy);
                }

                goldRemaining -= cardToBuy.cost;
                if (cardToBuy != Cards.FIREGEM) {
                    market.remove(cardToBuy);
                    cardsToBuy.remove(cardToBuy);
                }
                gameState.addBoughtCard(player, cardToBuy);
                didBuy = true;
            } else {
                didBuy = false;
            }
        } while (didBuy);

        boolean didTuck = false;
        List<Card> cardsToTuck = new ArrayList<>(market);
        List<Card> opponentTuckedCards = gameState.getTuckedCards(gameState.currentOpponent());
        do {
            // Filter to what is affordable and hasn't already been tucked by the opponent
            final int currentGoldRemaining = goldRemaining;
            cardsToTuck = cardsToTuck.stream()
                    .filter((c) -> c.cost <= currentGoldRemaining && !opponentTuckedCards.contains(c))
                    .collect(Collectors.toList());

            if (cardsToTuck.isEmpty()) {
                return;
            }

            // Ask the player
            Card cardToTuck = player.chooseTuck(cardsToTuck, gameState, goldRemaining);

            // tuck the card
            if (cardToTuck != null) {
                if (logChanges) System.out.println("Tucking card " + cardToTuck);
                goldRemaining -= cardToTuck.cost;
                gameState.addTuckedCard(player, cardToTuck);
                cardsToTuck.remove(cardToTuck);
                didTuck = true;
            } else {
                didTuck = false;
            }
        } while (didTuck && !cardsToTuck.isEmpty());

    }

    private void sacForDamage(List<Card> cardsInPlay, int remainingDamage) {
        HRPlayer player = gameState.currentPlayer();
        List<Card> sacableCards = cardsInPlay.stream().filter((c) -> c.sacrificeDamage() > 0).collect(Collectors.toList());
        int damage = remainingDamage;
        while (!sacableCards.isEmpty()) {
            // Ask the player
            Card toSac = player.chooseSacForDamage(sacableCards, gameState);

            // sac the card or bail
            if (toSac != null) {
                if (logChanges) System.out.println("Sacrificing for damage: " + toSac);
                damage += toSac.sacrificeDamage();
                cardsInPlay.remove(toSac);
                gameState.addPlayerSacrificed(player, toSac);
                sacableCards = cardsInPlay.stream().filter((c) -> c.sacrificeDamage() > 0).collect(Collectors.toList());
            } else {
                break;
            }
        }

        if (damage > 0) {
            // apply damage
            HRPlayer opponent = gameState.currentOpponent();
            List<Card> opponentChampions = gameState.getChampions(opponent);
            List<Card> opponentGuards = opponentChampions.stream().filter((c) -> c.isGuard).collect(Collectors.toList());
            Collections.sort(opponentGuards, new Cards.CostComparator());

            // Stun guards as possible
            if (!opponentGuards.isEmpty()) {
                for (int i=0; i<opponentGuards.size(); i++) {
                    Card champ = opponentGuards.get(i);
                    if (damage >= champ.championHealth) {
                        if (logChanges) System.out.println("Using damage to stun " + champ);
                        gameState.stunChampion(opponent, champ);
                        damage -= champ.championHealth;
                    }
                }
            }

            if (!opponentGuards.isEmpty()) {
                return;
            }

            if (logChanges) System.out.println("Damage to opponent " + damage);
            gameState.damagePlayer(opponent, damage);
            damage = 0;
            return;
        }
    }

    private int applyDamageIncludingSacrificing(List<Card> cardsInPlay, int totalDamage) {
        int remainingDamage = totalDamage;

        HRPlayer player = gameState.currentPlayer();
        HRPlayer opponent = gameState.currentOpponent();
        List<Card> opponentChampions = gameState.getChampions(opponent);
        List<Card> opponentGuards = opponentChampions.stream().filter((c) -> c.isGuard).collect(Collectors.toList());
        Collections.sort(opponentGuards, new Cards.CostComparator());

        // stun guards, sacrificing firegems if necessary
        List<Card> sacableCards = cardsInPlay.stream().filter((c) -> c.sacrificeDamage() > 0).collect(Collectors.toList());
        List<Card> firegems = sacableCards.stream().filter((c) -> c.name.equals("firegem")).collect(Collectors.toList());
        int totalFiregemSacDamage = firegems.stream().mapToInt((c) -> c.sacrificeDamage()).sum();
        for (int i=0; i<opponentGuards.size(); i++) {
            Card champ = opponentGuards.get(i);
            if (remainingDamage >= champ.championHealth) {
                if (logChanges) System.out.println("Using damage to stun " + champ);
                gameState.stunChampion(opponent, champ);
                remainingDamage -= champ.championHealth;
            } else if (remainingDamage + totalFiregemSacDamage >= champ.championHealth) {
                // Sacrifice all the firegems we need to sacrifice to kill this champion
                for (Card firegem : firegems) {
                    if (logChanges) System.out.println("Sacrificing for damage: " + firegem);
                    remainingDamage += firegem.sacrificeDamage();
                    totalFiregemSacDamage -= firegem.sacrificeDamage();
                    cardsInPlay.remove(firegem);
                    gameState.addPlayerSacrificed(player, firegem);
                    if (remainingDamage >= champ.championHealth) {
                        break;
                    }
                }

                if (logChanges) System.out.println("Using damage to stun " + champ);
                gameState.stunChampion(opponent, champ);
                remainingDamage -= champ.championHealth;
            }

//            checkDupes(gameState.getDiscardPile(opponent));
        }

        // if we still have guards, then we can't do damage to anything else
        opponentChampions = gameState.getChampions(opponent);
        opponentGuards = opponentChampions.stream().filter((c) -> c.isGuard).collect(Collectors.toList());
        if (!opponentGuards.isEmpty()) {
            return remainingDamage;
        }

        // if we can kill the player, including sacrificing if necessary, then do that
        sacableCards = cardsInPlay.stream().filter((c) -> c.sacrificeDamage() > 0).collect(Collectors.toList());
        int totalSacDamage = sacableCards.stream().mapToInt((c) -> c.sacrificeDamage()).sum();
        if (remainingDamage >= gameState.getPlayerHealth(opponent)) {
            if (logChanges) System.out.println("Damage to opponent " + remainingDamage);
            gameState.damagePlayer(opponent, remainingDamage);
            remainingDamage = 0;
            return remainingDamage;
        } else if (remainingDamage + totalSacDamage >= gameState.getPlayerHealth(opponent)) {
            // Sacrifice all the firegems we need to kill player
            for (Card sacableCard : sacableCards) {
                if (logChanges) System.out.println("Sacrificing for damage: " + sacableCard);
                remainingDamage += sacableCard.sacrificeDamage();
                cardsInPlay.remove(sacableCard);
                gameState.addPlayerSacrificed(player, sacableCard);
                if (remainingDamage >= gameState.getPlayerHealth(opponent)) {
                    break;
                }
            }

            if (logChanges) System.out.println("Damage to opponent " + remainingDamage);
            gameState.damagePlayer(opponent, remainingDamage);
            remainingDamage = 0;
            return remainingDamage;
        }

        // stun remaining champions, including sacrificing if necessary
        sacableCards = cardsInPlay.stream().filter((c) -> c.sacrificeDamage() > 0).collect(Collectors.toList());
        firegems = sacableCards.stream().filter((c) -> c.name.equals("firegem")).collect(Collectors.toList());
        totalFiregemSacDamage = firegems.stream().mapToInt((c) -> c.sacrificeDamage()).sum();
        // copy list so we can iterate it without champions we stun disappearing
        opponentChampions = new ArrayList<>(opponentChampions);
        for (int i=0; i<opponentChampions.size(); i++) {
            Card champ = opponentChampions.get(i);
            if (remainingDamage >= champ.championHealth) {
                if (logChanges) System.out.println("Using damage to stun " + champ);
                gameState.stunChampion(opponent, champ);
                remainingDamage -= champ.championHealth;
            } else if (remainingDamage + totalFiregemSacDamage >= champ.championHealth) {
                // Sacrifice all the firegems we need to sacrifice to kill this champion
                for (Card firegem : firegems) {
                    if (logChanges) System.out.println("Sacrificing for damage: " + firegem);
                    remainingDamage += firegem.sacrificeDamage();
                    totalFiregemSacDamage -= firegem.sacrificeDamage();
                    cardsInPlay.remove(firegem);
                    gameState.addPlayerSacrificed(player, firegem);
                    if (remainingDamage >= champ.championHealth) {
                        break;
                    }
                }

                if (logChanges) System.out.println("Using damage to stun " + champ);
                gameState.stunChampion(opponent, champ);
                remainingDamage -= champ.championHealth;
            }

//            checkDupes(gameState.getDiscardPile(opponent));
        }

        // apply remaining damage to player if no guards
        if (remainingDamage > 0) {
            if (logChanges) System.out.println("Damage to opponent " + remainingDamage);
            gameState.damagePlayer(opponent, remainingDamage);
            remainingDamage = 0;
        }

        return remainingDamage;
    }

    private void gainHealth(TurnSummary turnSummary) {
        if (turnSummary.health > 0) {
            if (logChanges) System.out.println("Gaining health " + turnSummary.health);
            gameState.gainHealth(gameState.currentPlayer(), turnSummary.health);
        }
    }

    private List<Card> discardCards(List<Card> hand, int cardsToDiscard) {
        if (cardsToDiscard > 0) {
            Collections.sort(hand, new Cards.DiscardComparator());
            while (cardsToDiscard > 0 && hand.size() > 0) {
                if (logChanges) System.out.println("Discarding " + hand.get(0));
                hand.remove(0);
                cardsToDiscard--;
            }
        }
        return hand;
    }

    private List<CardWrapper> discardCardWrappers(List<CardWrapper> wrappers, int cardsToDiscard) {
        if (cardsToDiscard > 0) {
            Collections.sort(wrappers, new Cards.WrapperDiscardComparator());
            while (cardsToDiscard > 0 && wrappers.size() > 0) {
                if (logChanges) System.out.println("Discarding " + wrappers.get(0).card);
                wrappers.remove(0);
                cardsToDiscard--;
            }
        }
        return wrappers;
    }

    public void play() {
        while (!isTerminated()) {
            executeCurrentTurn();
            gameState.incrementTurnNumber();

//            // remove - just here for debugging
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

    public boolean isTerminated() {
        for (HRPlayer player : players) {
            if (gameState.getPlayerHealth(player) <= 0) {
                return true;
            }
        }
        if (isStalemate()) return true;
        return false;
    }

    private boolean isStalemate() {
        return gameState.getTurnNumber() >= MAXIMUM_NUMBER_OF_TURNS;
    }

    public String getWinner() {
        if (!isTerminated()) {
            throw new IllegalStateException();
        }

        if (isStalemate()) {
            return null;
        }

        for (HRPlayer player : players) {
            if (gameState.getPlayerHealth(player) > 0) {
                return player.getName();
            }
        }
        throw new IllegalStateException();
    }

    private void validateNames(List<HRPlayer> players) {
        Set<String> names = new HashSet<>();
        for (HRPlayer player : players) {
            if (names.contains(player.getName())) {
                throw new RuntimeException("No duplicate player names allowed. Saw more than one of: " + player.getName());
            }
            names.add(player.getName());
        }
    }

    public void setLogChanges(boolean doLog) {
        logChanges = doLog;
    }

    public HRGameState getState() {
        return gameState;
    }

    public static final class CardWrapper {
        public final Card card;
        public boolean hasCheckedForDraw = false;
        public boolean hasCheckedForDrawDiscard = false;
        public boolean hasPlayed = false;

        public CardWrapper(Card card) {
            this.card = card;
        }
        public CardWrapper(Card card, boolean alreadyPlayed) {
            this.card = card;
            this.hasPlayed = alreadyPlayed;
        }

        public static List<Card> toCards(List<CardWrapper> wrappers) {
            return wrappers.stream().map(w -> w.card).collect(Collectors.toList());
        }
    }
}
