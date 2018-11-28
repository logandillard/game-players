package games;

public interface GamePlayer<G extends Game> {
	Move move(G theGame) throws Exception ;
}
