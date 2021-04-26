package org.example.babich.domain;

import java.util.Objects;

/**
 * news that comes from publishers
 * <p></p>{@code range } - The priority of a news item within the range [0..9]
 * <p></p>{@code headline } - The headline of a news item should be a random combination of three to five words from the
 * following list: up, down, rise, fall, good, bad, success, failure, high, low, über, unter.
 *
 */
public class News {
    private final int priority;
    private final String headline;

    public News(int priority, String headline) {
        if(9 < priority || priority < 0){
            throw new IllegalArgumentException("The priority of a news item within the range [0..9], but actually "
                    + priority);
        }

        this.priority = priority;
        this.headline = headline;
    }

    public int getPriority() {
        return priority;
    }

    public String getHeadline() {
        return headline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        News that = (News) o;
        return Objects.equals(headline, that.headline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headline);
    }

    @Override
    public String toString() {
        return "PieceOfNews{" +
                "range=" + priority +
                ", header='" + headline + '\'' +
                '}';
    }
}
