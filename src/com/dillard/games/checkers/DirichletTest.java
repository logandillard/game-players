package com.dillard.games.checkers;

public class DirichletTest {
    public static void main(String[] args) {
        Dirichlet diri = new Dirichlet(new double[] {0.3, 0.3, 0.3, 0.3, 0.3});
        for (int i=0; i<100; i++) {
            StringBuilder sb = new StringBuilder();
            double[] ds = diri.nextDistribution();
            for (int j=0; j<ds.length; j++) {
                sb.append(String.format("%.3f, ", ds[j]));
            }
            System.out.println(sb.toString());
        }
    }
}
