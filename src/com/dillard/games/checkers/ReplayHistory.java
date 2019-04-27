package com.dillard.games.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class ReplayHistory {
    private Random random;
    private int maxSize;
    private List<TrainingExample> replayHistory = null;
    private PrioritizedSampler prioritizedSampler;
    private boolean doPrioritySampling = false;
    private final double importanceSamplingBiasExponent = 0.5; // anneal from 0.4 to 1.0
    private final double priorityExponent = 0.5;

    public ReplayHistory(Random random, int maxSize) {
        this(random, maxSize, null);
    }

    public ReplayHistory(Random random, int maxSize, List<TrainingExample> history) {
        this.random = random;
        this.maxSize = maxSize;
        this.replayHistory = history;
        if (this.replayHistory == null) {
            this.replayHistory = new ArrayList<>();
        }
        prioritizedSampler = new PrioritizedSampler(random);
        prioritizedSampler.addAll(replayHistory);
    }

    public synchronized void add(List<TrainingExample> newTrainingExamples) {
        replayHistory.addAll(newTrainingExamples);
        // Truncate replay history (global)
        if (replayHistory.size() > maxSize) {
            replayHistory = new ArrayList<>(replayHistory.subList(maxSize / 10, replayHistory.size()));
            if (doPrioritySampling) {
                prioritizedSampler = new PrioritizedSampler(random);
                prioritizedSampler.addAll(replayHistory);
            }
        } else {
            if (doPrioritySampling) {
                prioritizedSampler.addAll(newTrainingExamples);
            }
        }
    }

    public synchronized List<TrainingExample> sample(final int miniBatchSize) {
        List<TrainingExample> miniBatch = new ArrayList<>();

        if (doPrioritySampling) {
            double totalPrioritySum = prioritizedSampler.getPrioritySum();
            int totalCount = prioritizedSampler.getNodeCount();
            if (totalCount == 0) {
                // this at least won't work for the importance weight below, where we try to divide by this
                throw new RuntimeException("Trying to sample from prioritized sampler with 0 nodes!");
            }
            double maxImportanceWeight = 0;
            for (int i=0; i<miniBatchSize; i++) {
                TrainingExample te = prioritizedSampler.sampleAndRemove();
                te.importanceWeight = Math.pow(totalPrioritySum / (te.priority * totalCount), importanceSamplingBiasExponent);
                if (Double.isNaN(te.importanceWeight)) {
                    throw new RuntimeException();
                }
                miniBatch.add(te);
                if (te.importanceWeight > maxImportanceWeight) {
                    maxImportanceWeight = te.importanceWeight;
                }
            }

            if (maxImportanceWeight == 0.0) {
                System.out.println(miniBatch); // TODO
                throw new RuntimeException("Max importance weight is 0!");
            }

            // Scale weights by 1/maxImportanceWeight so that they only scale down for stability
            for (var te : miniBatch) {
                te.importanceWeight = te.importanceWeight / maxImportanceWeight;
            }
        } else {
            // Uniform random sampling
            for (int i=0; i<miniBatchSize; i++) {
                int idx = random.nextInt(replayHistory.size());
                var te = replayHistory.get(idx);
                te.importanceWeight = 1.0;
                miniBatch.add(te);
            }
        }
        return miniBatch;
    }

    public List<TrainingExample> getReplayHistory() {
        return replayHistory;
    }

    public boolean isEmpty() {
        return this.replayHistory.isEmpty();
    }

    public void reweightPriority(List<TrainingExample> miniBatch, Function<TrainingExample, Double> scorer) {
        if (doPrioritySampling) {
            // re-score examples in miniBatch
            for (var te : miniBatch) {
                te.priority = Math.pow(Math.abs(scorer.apply(te)), priorityExponent);
            }
            prioritizedSampler.addAll(miniBatch);
        }
    }

    public int size() {
        return replayHistory.size();
    }
}
