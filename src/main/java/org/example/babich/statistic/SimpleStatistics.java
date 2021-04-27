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

    /**
     * This method is run by the scheduler and collects statistics for the consumer.
     *
     */
    @Override
    public void run() {
        //Started it off as an optimistic reading, because the writing incoming data is more important then collect statistics
        long stamp = lock.tryOptimisticRead();

        long counterValue = newsCounter;
        NavigableMap<News, Integer> newsIntegerNavigableMap = topPositiveNews;

        try {
            //after collecting data, tries to reset existing data.
            //Copied the link to the rating map and the value for the counter.
            // In this case, the copied counter value may differ from the contents of the rating map,
            // because the map is still receiving messages, and the previously used counter is also incremented,
            // and this appears to be an inconsistent state of the counter and rating map.
            //
            // Then I check this situation - were there any updates while I copied the counter value and the map reference?
            // If not (optimistic case) - I get a write lock and reset the counter and card.
            // If any update has happened, a pessimistic scenario is applied and a write lock to read / update the data.
            //
            // Maybe in this case it is not necessary and pessimistic locking can be used every time for these updates,
            // but I wanted to demonstrate this approach.
            while (true) {
                //trying to get write lock
                long ws = lock.tryConvertToWriteLock(stamp);
                //if there are new data in the statistics, read them again
                if (ws == 0L) {
                    stamp = lock.writeLock();
                    counterValue = newsCounter;
                    newsIntegerNavigableMap = topPositiveNews;
                    continue;
                }

                //reset existing data.
                stamp = ws;
                topPositiveNews = createReference(pieceOfNewsComparator);
                newsCounter = 0;
                break;
            }
        } finally {
            //reset lock )
            lock.unlock(stamp);
        }
        //provide the consumer with statistics.
        statConsumer.accept(toSimpleStatisticsResult(counterValue, newsIntegerNavigableMap));

    }

    /**
     * Put new message to statistic.
     * @param message message from socket worker
     */
    @Override
    public void onPositiveNews(SocketMessage<News> message) {
        try {
            //waiting for write lock to increase counter and update message priority
            // There is a synchronized block, can be used instead of tryWriteLock.
            // But I think it's really important not to block the socket worker thread for a unpredictable period of time,
            // for this reason tryLock has a timeout.
            if (0 == lock.tryWriteLock(50, TimeUnit.MILLISECONDS)) {
                //There may be an exception because the message was lost.
                // But i preferred to implement it as a quiet return.
                return;
            }
            //increase the positive news counter
            newsCounter++;

            //update message priority if the same message has already been received with a different priority.
            //and put if it is a new one
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
            //set interrupted flag of thread if the current thread was interrupted while it was waiting for a write lock.
            Thread.currentThread().interrupt();
        } finally {
            //unlock write lock )
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
