package org.example.babich.newsfeed;

import java.util.Random;
import java.util.function.Supplier;

/**
 * news generation interval in ms between message sending, for news publisher.
 */
public class RandomlyDelaySupplier implements Supplier<Long> {

    private final int interval;
    private final Random random;

    public RandomlyDelaySupplier(int maxInterval) {
        if(0 >= maxInterval){
            throw new IllegalArgumentException("Interval cannot be less then 0.");
        }
        this.interval = maxInterval;
        this.random = new Random();
    }


    @Override
    public Long get() {
        return (long) random.nextInt(interval);
    }
}
