package games;

public class LayeredNNTestMain {
    public static void main(String[] args) {
        LayeredNN nn = new LayeredNN(new int[] {2, 3, 1}, new ActivationFunctionTanH(),
                new WeightInitializerGaussianFixedVariance(1.0/5.0),
                0.1, 1.0, 0, 0.0001);
        nn.reset();

        for (int e=0; e<10; e++) {
            int[] stats = {0, 0};

            for (int i=0; i<100000; i++) {
                double[] inputValues = {Math.random(), Math.random()};
//                double[] inputValues = {Math.random()};
//                double correctScore = inputValues[0] > 0.5 ? 1 : -1;
                double correctScore = inputValues[0] > 0.3 && inputValues[1] > 0.3 ? 1 : -1;

                nn.reset();
                double score = nn.activate(inputValues)[0];

//                System.out.println(score + " " + correctScore);
                nn.updateElig();
                nn.tdLearn(new double[]{correctScore - score});

                double updatedScore = nn.activate(inputValues)[0];

                if (correctScore == 1.0) {
                    if (score > 0.0) {
                        stats[0]++;
                    } else {
                        stats[1]++;
                    }
                } else {
                    if (score < 0.0) {
                        stats[0]++;
                    } else {
                        stats[1]++;
                    }
                }
            }

            System.out.println(stats[0] / (double) (stats[0] + stats[1]));
        }

        System.out.println(nn);
    }
}
