package org.example.babich.server;

import org.example.babich.converter.NewsConverter;
import org.example.babich.domain.News;
import org.example.babich.messages.EventListener;
import org.example.babich.messages.EventPublisher;
import org.example.babich.messages.SocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * It is a class that receives messages from a socket queue, transforms it into a domain object, and sends it to subscribers.
 * <p></p>{@code queue } - socket data queue where the server puts incoming messages
 * <p></p>{@code eventListeners } - collection of domain object consumers
 * <p></p>{@code messageConverter } - converting message from socket data format (string) to domain object.
 * <p></p>{@code messageFilter } - filter of incoming messages.
 */
public class SocketDataConsumer implements Runnable, EventPublisher<News> {

    private final static Logger logger = LoggerFactory.getLogger(SocketDataConsumer.class);

    private final BlockingQueue<ByteBuffer> queue;
    private final List<EventListener<News>> eventListeners = new CopyOnWriteArrayList<>();
    private final Function<ByteBuffer, News> messageConverter;
    private final Predicate<News> messageFilter;


    private SocketDataConsumer(BlockingQueue<ByteBuffer> queue
            , Function<ByteBuffer, News> messageConverter
            , Predicate<News> messageFilter) {

        this.queue = queue;
        this.messageFilter = messageFilter;
        this.messageConverter = messageConverter;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            ByteBuffer message;
            try {
                message = queue.poll(100, TimeUnit.MILLISECONDS);

                if (null == message) {
                    continue;
                }

                News receivedNews = messageConverter.apply(message);

                if (messageFilter.test(receivedNews)) {
                    sendMessage(receivedNews);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e){
                logger.error("An error occurred while processing a socket message.", e);
            }
        }
    }

    @Override
    public void addEventListener(EventListener<News> eventListener) {
        eventListeners.add(eventListener);
    }

    void sendMessage(News receivedNews) {
        SocketMessage<News> newsSocketMessage = new SocketMessage<>(receivedNews);

        eventListeners.forEach(pieceOfNewsEventListener ->
                pieceOfNewsEventListener.onPositiveNews(newsSocketMessage));
    }


    private static ExecutorService getExecutorService(){
        ExecutorService executorService = Executors.newWorkStealingPool();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.debug("stopping socket data consumer pool...");
                executorService.shutdown();
                if(!executorService.awaitTermination(3, TimeUnit.SECONDS)){
                    executorService.shutdownNow();
                }
                logger.debug("stopped socket data consumer pool...");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        return executorService;
    }

    public static Consumer<ByteBuffer> newSocketDataConsumer(Consumer<SocketDataConsumerConfigurer> configurerConsumer
            , EventListener<News> analyser) {

        SocketDataConsumerConfigurer configurer = new SocketDataConsumerConfigurer();
        configurerConsumer.accept(configurer);

        ExecutorService executorService = getExecutorService();
        IntStream.range(0, Runtime.getRuntime().availableProcessors())
                .forEach(i -> {
                    SocketDataConsumer dataConsumer = configurer.build();
                    dataConsumer.addEventListener(analyser);
                    executorService.submit(dataConsumer);
                });

        return byteBuffer -> configurer.queue.offer(byteBuffer);
    }

    public static class SocketDataConsumerConfigurer {
        private BlockingQueue<ByteBuffer> queue;
        private Function<ByteBuffer, News> messageConverter;
        private Predicate<News> messageFilter;

        private static class BlockingQueueFactory {
            private static final BlockingQueue<ByteBuffer> QUEUE_INSTANCE = new LinkedBlockingQueue<>();
        }

        public SocketDataConsumerConfigurer withQueue(BlockingQueue<ByteBuffer> queue) {
            this.queue = queue;
            return this;
        }

        public SocketDataConsumerConfigurer withConverter(Function<ByteBuffer, News> messageConverter) {
            this.messageConverter = messageConverter;
            return this;
        }

        public SocketDataConsumerConfigurer messageFilter(Predicate<News> messageFilter) {
            this.messageFilter = messageFilter;
            return this;
        }

        Function<ByteBuffer, News> getDefaultMessageConverter() {
            return new NewsConverter();
        }

        SocketDataConsumer build() {
            if (null == queue) {
                queue = BlockingQueueFactory.QUEUE_INSTANCE;
            }

            if (null == messageConverter) {
                messageConverter = getDefaultMessageConverter();
            }

            if (null == messageFilter) {
                messageFilter = (item) -> true;
            }

            return new SocketDataConsumer(queue, messageConverter, messageFilter);
        }

    }

}
