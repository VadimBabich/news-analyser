package org.example.babich.statistic;

import org.example.babich.domain.News;

import java.time.LocalDateTime;
import java.util.List;

/**
 * It is a statistical summary object that passed from the SimpleStatistics class to the consumer.
 * <p></p>{@code time} - time when statistics were collected
 * <p></p>{@code sortedNews } - the unique headlines of up to three of the highest-priority positive news
 * <p></p>{@code totalPositiveNews } - the count of positive news items seen during the interval
 */
public class SimpleStatisticsResult {

    private final LocalDateTime time;
    private final List<News> sortedNews;
    private final Long totalPositiveNews;

    public SimpleStatisticsResult(LocalDateTime time, Long totalPositiveNews, List<News> sortedNews) {
        this.time = time;
        this.sortedNews = sortedNews;
        this.totalPositiveNews = totalPositiveNews;
    }

    public List<News> getSortedNews() {
        return sortedNews;
    }

    public Long getTotalPositiveNews() {
        return totalPositiveNews;
    }

    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "SimpleStatisticsResult{" +
                "time=" + time +
                ", sortedNews=" + sortedNews +
                ", totalPositiveNews=" + totalPositiveNews +
                '}';
    }
}
