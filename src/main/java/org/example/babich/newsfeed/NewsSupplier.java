package org.example.babich.newsfeed;

import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The priority of a news item should be an integer within the range [0..9].
 * News messages with higher priority should be generated with less probability than those with lower priority.
 * The headline of a news item should be a random combination of three to five words from the following list: {@code headlineWords}
 */
public class NewsSupplier implements Supplier<String> {

    int[] priorityDistributionMap;
    final int distributionSize = 100;
    final int priorityCount = 10;

    private final Random random;
    private final String[] headlineWords;


    public NewsSupplier(String[] headlineWords) {

        if (null == headlineWords || headlineWords.length == 0) {
            throw new IllegalArgumentException("");
        }

        this.headlineWords = headlineWords;
        this.random = new Random();

        priorityDistributionMap = generateDistributionMap(distributionSize, priorityCount);
    }

    @Override
    public String get() {
        return String.format("{%d}{%s}", generatePriorityValue(), generateHeadline());
    }


    private int generatePriorityValue() {
        return priorityDistributionMap[random.nextInt(distributionSize)];
    }

    private String generateHeadline() {
        return IntStream.range(0, random.nextInt(3) + 3)
                .mapToObj(i -> headlineWords[random.nextInt(headlineWords.length)])
                .collect(Collectors.joining(" "));
    }


    static int[] generateDistributionMap(int distributionSize, int priorityCount) {
        int[] linearProbabilityDispersion = generateLinearDisproportion(distributionSize, priorityCount);
        return getPriorityDistributionMap(distributionSize, priorityCount, linearProbabilityDispersion);
    }

    static int[] generateLinearDisproportion(int distributionSize, int priorityCount) {
        int min = distributionSize / (priorityCount * 2);
        int[] result = new int[priorityCount];
        result[0] = min;
        result[priorityCount - 1] = distributionSize;
        for (int i = 1; i < priorityCount - 1; i++) {
            result[i] = result[i - 1] + min + i;
        }
        return result;
    }

    static int[] getPriorityDistributionMap(int interval, int priorityCount, int[] disproportion) {
        int[] result = new int[interval];
        for (int i = 0, k = 0; i < interval; i++) {
            if (i > disproportion[k]) {
                k++;
            }
            result[i] = priorityCount - k - 1;
        }
        return result;
    }
}
