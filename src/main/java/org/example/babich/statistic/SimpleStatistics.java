package org.example.babich.statistic;

import org.example.babich.domain.News;
import org.example.babich.messages.EventListener;
import org.example.babich.messages.SocketMessage;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This is a news statistic that receives messages from the server and sends the summary result to the consumer.
 * <p></p>{@code newsLimit} - limit of the highest-priority positive news that should appear in summary result.
 * <p></p>{@code newsCounter} - the count of positive news items seen during the last 10 seconds.
 * <p></p>{@code pieceOfNewsComparator} - news priority comparator. (first by priority then by text of header)
 * <p></p>{@code statConsumer} - consumer of statistic summary.
 *
 */
public class SimpleStatistics implements EventListener<News>, Runnable, Closeable {

    private final Consumer<SimpleStatisticsResult> statConsumer;
    private NavigableMap<News, Integer> topPositiveNews;
    private final Comparator<News> pieceOfNewsComparator;
    private final int newsLimit;
    private long newsCounter = 0;

    private ScheduledFuture<?> future;
    private final StampedLock lock = new StampedLock();

    private SimpleStatistics(Consumer<SimpleStatisticsResult> statConsumer
            , Comparator<News> pieceOfNewsComparator
            , int newsLimit) {
        this.newsLimit = newsLimit;
        this.statConsumer = statConsumer;
        this.pieceOfNewsComparator = pieceOfNewsComparator;
        this.topPositiveNews = createReference(pieceOfNewsComparator);
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    static <K, V> NavigableMap<K, V> createReference(Comparator<K> pieceOfNewsComparator) {
        return new TreeMap<>(pieceOfNewsComparator);
    }

    @Override
    public void close() {
        future.cancel(false);
    }

    @Override
    public void run() {
        long stamp = lock.tryOptimisticRead();

        long counterValue = newsCounter;
        NavigableMap<News, Integer> newsIntegerNavigableMap = topPositiveNews;

        try {
            while (true) {
                long ws = lock.tryConvertToWriteLock(stamp);

                if (ws == 0L) {
                    stamp = lock.writeLock();
                    counterValue = newsCounter;
                    newsIntegerNavigableMap = topPositiveNews;
                    continue;
                }

                stamp = ws;
                topPositiveNews = createReference(pieceOfNewsComparator);
                newsCounter = 0;
                break;
            }
        } finally {
            lock.unlock(stamp);
        }
        statConsumer.accept(toSimpleStatisticsResult(counterValue, newsIntegerNavigableMap));

    }

    @Override
    public void onPositiveNews(SocketMessage<News> message) {
        try {
            if (0 == lock.tryWriteLock(50, TimeUnit.MILLISECONDS)) {
                return;
            }
            newsCounter++;

            topPositiveNews.compute(message.getPayload(), (pieceOfNews, priority) -> {
                if (null == priority) {
                    return message.getPayload().getPriority();
                }
                if (priority >= message.getPayload().getPriority()) {
                    return priority;
                }
                return message.getPayload().getPriority();
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.tryUnlockWrite();
        }
    }

    static public SimpleStatistics newSimpleStatistics(Consumer<SimpleStatistics.SimpleStatisticsBuilder> builder) {
        SimpleStatistics.SimpleStatisticsBuilder newBuilder = new SimpleStatistics.SimpleStatisticsBuilder();
        builder.accept(newBuilder);
        return newBuilder.build();
    }


    private SimpleStatisticsResult toSimpleStatisticsResult(Long count
            , NavigableMap<News, Integer> listOfPositiveNew) {

        List<News> result = listOfPositiveNew.descendingKeySet().stream()
                .limit(newsLimit)
                .collect(Collectors.toList());

        return new SimpleStatisticsResult(LocalDateTime.now(), count, result);
    }

    public static class SimpleStatisticsBuilder {
        private int delay;
        private int newsLimit;
        private Consumer<SimpleStatisticsResult> statConsumer;
        private Comparator<News> newsComparator;

        public SimpleStatisticsBuilder() {
            this.delay = 10;
            this.newsLimit = 3;
        }

        public SimpleStatistics.SimpleStatisticsBuilder withDaley(int sec) {
            this.delay = sec;
            return this;
        }

        public SimpleStatistics.SimpleStatisticsBuilder withConsumer(Consumer<SimpleStatisticsResult> statConsumer) {
            this.statConsumer = statConsumer;
            return this;
        }

        public SimpleStatistics.SimpleStatisticsBuilder withNewsLimit(int newsLimit) {
            this.newsLimit = newsLimit;
            return this;
        }

        public SimpleStatistics.SimpleStatisticsBuilder withNewsComparator(Comparator<News> newsComparator) {
            this.newsComparator = newsComparator;
            return this;
        }

        Comparator<News> getDefaultNewsComparator() {
            return Comparator.comparingInt(News::getPriority)
                    .thenComparing(News::getHeadline);
        }


        private SimpleStatistics build() {
            if (0 >= delay) {
                throw new IllegalStateException("delay cannot be less or eq zero.");
            }

            if (0 >= newsLimit) {
                throw new IllegalStateException("newsLimit cannot be less or eq zero.");
            }

            if (null == statConsumer) {
                throw new IllegalStateException("statConsumer cannot be null.");
            }

            if (null == newsComparator) {
                newsComparator = getDefaultNewsComparator();
            }

            SimpleStatistics statistics = new SimpleStatistics(statConsumer, newsComparator, newsLimit);

            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            statistics.setFuture(service.scheduleAtFixedRate(statistics
                    , delay
                    , delay
                    , TimeUnit.SECONDS));

            Runtime.getRuntime().addShutdownHook(new Thread(service::shutdownNow));

            return statistics;
        }
    }
}
