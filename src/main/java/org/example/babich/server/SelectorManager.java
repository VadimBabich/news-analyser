package org.example.babich.server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

public class SelectorManager implements Closeable {

    private static final int DEFAULT_BUF_SIZE = 2 * 1024;

    private final Selector selector;
    private final ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUF_SIZE);
    private final Consumer<ByteBuffer> dataConsumer;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    public SelectorManager(Selector selector, Consumer<ByteBuffer> dataConsumer) {
        this.selector = selector;
        this.dataConsumer = dataConsumer;
    }

    public void doSelect() throws IOException {
        while (true) {

            if (0 == selector.select()) {
                continue;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();

            for (Iterator<SelectionKey> iterator = readyKeys.iterator(); iterator.hasNext(); iterator.remove()) {
                SelectionKey key = iterator.next();
                
                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    doAccept(key);
                    continue;
                }

                if (key.isReadable()) {
                    doRead(key);
                }
            }
        }
    }

    public Selector getSelector() {
        return selector;
    }

    @Override
    public void close() throws IOException {
        if (null != selector) {
            selector.close();
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();

        client.configureBlocking(false);

        if (logger.isDebugEnabled()) {
            Socket socket = client.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            logger.debug("Connected to: {}", remoteAddr);
        }

        client.register(selector, SelectionKey.OP_READ);
    }

    private void doRead(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        try {
            int numRead = client.read(buffer);
            if (-1 == numRead) {
                clientCloseConnection(key, client);
                return;
            }

            byte[] data = new byte[numRead];
            System.arraycopy(buffer.array(), 0, data, 0, numRead);

            dataConsumer.accept(ByteBuffer.wrap(data));
        } catch (Exception e) {
            logger.error("Error while reading client message", e);
        } finally {
            buffer.clear();
        }

    }

    void clientCloseConnection(SelectionKey key, SocketChannel client) throws IOException {

        if (logger.isDebugEnabled()) {
            Socket socket = client.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            logger.debug("Connection closed by client: {}", remoteAddr);
        }

        client.close();
        key.cancel();
    }
}
