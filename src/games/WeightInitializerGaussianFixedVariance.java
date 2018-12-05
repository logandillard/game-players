package games;

import java.util.Random;

public class WeightInitializerGaussianFixedVariance implements WeightInitializer {
    private double variance;
    private Random rand;

    public WeightInitializerGaussianFixedVariance(double variance) {
        this.variance = variance;
        this.rand = new Random(972384l);
    }
    
    public WeightInitializerGaussianFixedVariance(double variance, Random rand) {
        this.variance = variance;
        this.rand = rand;
    }

    @Override
    public double nextInitialWeight() {
        return rand.nextGaussian() * variance;
    }

}
