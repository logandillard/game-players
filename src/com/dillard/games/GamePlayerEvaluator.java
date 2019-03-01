package com.dillard.games;


public class GamePlayerEvaluator {
	public static final int BENCHMARK_GAMES = 1000;
	public static final int TIMING_TEST_MOVES = 1000;

	public static <M extends Move, G extends Game<M, G>> void evaluate(G game,
			                    NNPlayer<M, G> player,
			                    GamePlayer<M, G>[] benchOpponents,
			                    String[] benchOpponentsNames)
	throws Exception {
		double[][] benchmarkResults = null;

		System.out.println("Benchmarking... \n");

		// Benchmark
		benchmarkResults = NNPlayerTrainer.benchmark(game, player, BENCHMARK_GAMES, benchOpponents);

		System.out.println(
				NNPlayerTrainer.benchmarkResultsDisplay(BENCHMARK_GAMES, benchmarkResults, benchOpponentsNames));

		System.out.println("\nDoing timing test...\n");

		long[] benchOppTimes = new long[benchOpponents.length];
		long myTime, start, end;

		// Time test opponents
		for(int i=0; i< benchOppTimes.length; i++) {

			start = System.currentTimeMillis();
			for(int j=0; j<TIMING_TEST_MOVES; j++) {
				benchOpponents[i].move(game);
			}
			end = System.currentTimeMillis();

			benchOppTimes[i] = end - start;
		}

		// Time test me
		start = System.currentTimeMillis();
		for(int j=0; j<TIMING_TEST_MOVES; j++) {
			player.move(game);
		}
		end = System.currentTimeMillis();

		myTime = end - start;

		//Print out times
		System.out.println("\nPlayer\tTime");
		System.out.println("ABNN\t"+ myTime);
		for(int i=0; i<benchOpponents.length; i++) {
			System.out.println(benchOpponentsNames[i] + "\t" + benchOppTimes[i]);
		}
	}
}
