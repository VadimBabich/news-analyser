package org.example.babich.newsfeed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *  Creates a persistent TCP connection to the news analyser and periodically sends news items over that connection.
 */
public class NewsPublisher implements Runnable{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InetSocketAddress hostAddress;
    private final Supplier<String> newsSupplier;
    private final Supplier<Long> delaySupplier;


    private NewsPublisher(InetSocketAddress hostAddress
            , Supplier<String> newsSupplier
            , Supplier<Long> delaySupplier) {

        this.delaySupplier = delaySupplier;
        this.hostAddress = hostAddress;
        this.newsSupplier = newsSupplier;
    }


    static SocketChannel getClient(InetSocketAddress hostAddress) throws IOException {
        return SocketChannel.open(hostAddress);
    }

    static void sendMessage(SocketChannel client, String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(message.getBytes().length);
        buffer.put(message.getBytes());
        buffer.flip();
        client.write(buffer);
        buffer.clear();
    }

    @Override
    public void run() {
        try (SocketChannel socketChannel = getClient(hostAddress)) {
            logger.debug("news publisher started for {}", hostAddress);

            while (!Thread.interrupted()){
                sendMessage(socketChannel, newsSupplier.get());
                TimeUnit.MILLISECONDS.sleep(delaySupplier.get());
            }
        } catch (IOException e) {
            logger.error("An error occurred while trying to establish a connection with {}", hostAddress, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static public NewsPublisher newNewsPublisher(Consumer<NewsPublisherConfigurer> configurer){
        NewsPublisherConfigurer publisherConfigurer = new NewsPublisherConfigurer();
        configurer.accept(publisherConfigurer);
        return publisherConfigurer.build();
    }

    public static class NewsPublisherConfigurer{
        private int port;
        private int daley;
        private String host;
        private String[] headlineWords;

        public NewsPublisherConfigurer withPort(int port){
            this.port = port;
            return this;
        }

        public NewsPublisherConfigurer withHost(String host){
            this.host = host;
            return this;
        }

        public NewsPublisherConfigurer withHeadlineWords(String[] headlineWords){
            this.headlineWords = headlineWords;
            return this;
        }

        public NewsPublisherConfigurer withDaley(int daley){
            this.daley = daley;
            return this;
        }

        NewsPublisher build(){
            if(0 > port || port > 65_535){
                throw new IllegalArgumentException("Port value cannot be great then 65 535 or less then 0.");
            }

            if(null == host || host.isEmpty()){
                host = "localhost";
            }

            if(0 >= daley){
                throw new IllegalArgumentException("Daley cannot be less then 0.");
            }

            if(null == headlineWords || headlineWords.length < 1){
                throw new IllegalArgumentException("The headline word set must contain more than one word.");
            }

            return new NewsPublisher(new InetSocketAddress(host, port)
                    , new NewsSupplier(headlineWords)
                    , new RandomlyDelaySupplier(daley));
        }
    }
}
