package org.example.babich.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public class Server {

    private final Consumer<ByteBuffer> dataConsumer;
    private final InetSocketAddress hostAddress;

    private final AtomicBoolean running = new AtomicBoolean();

    private Server(InetSocketAddress hostAddress
            , Consumer<ByteBuffer> dataConsumer) {

        this.hostAddress = hostAddress;
        this.dataConsumer = dataConsumer;
    }

    public boolean isRunning() {
        return running.get();
    }

    public InetSocketAddress getHostAddress() {
        return hostAddress;
    }

    public void doStart() {
        setStatusRunningOrException();

        try (SelectorManager manager = new SelectorManager(Selector.open(), dataConsumer);
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {

            configureServerChannel(serverChannel, manager.getSelector());

            manager.doSelect();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void setStatusRunningOrException() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server is already running.");
        }
    }

    private void configureServerChannel(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        serverChannel.configureBlocking(false);
        serverChannel.bind(hostAddress);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public static Server newServer(Consumer<ServerBuilder> serverBuilder){
        ServerBuilder builder = new ServerBuilder();
        serverBuilder.accept(builder);
        return builder.build();
    }


    public static class ServerBuilder {

        private Consumer<ByteBuffer> dataConsumer;
        private String host;
        private int port;

        public ServerBuilder withDataConsumer(Consumer<ByteBuffer> dataConsumer){
            this.dataConsumer = dataConsumer;
            return this;
        }

        public ServerBuilder withHost(String host){
            this.host = host;
            return this;
        }

        public ServerBuilder withPort(int port){
            this.port = port;
            return this;
        }

        Server build(){
            if(null == dataConsumer){
                throw new IllegalArgumentException("dataConsumer cannot be null.");
            }

            if(null == host || host.isEmpty()){
                host = "localhost";
            }

            if(0 > port || port > 65_535){
                throw new IllegalArgumentException("Port value cannot be great then 65 535 or less then 0.");
            }


            return new Server(new InetSocketAddress(host, port), dataConsumer);
        }
    }

}
