package com.dillard.games.checkers;

import java.io.Serializable;

public final class Scored<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    public final T value;
    public double score;

    public Scored(T value, double score) {
        this.value = value;
        this.score = score;
    }

    @Override
    public String toString() {
        return String.format("%f  %s", score, value.toString());
    }
}