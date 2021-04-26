package org.example.babich.messages;

import java.time.LocalDateTime;

/**
 * This is the message sent after parsing the socket buffers.
 * @param <T> payload of message
 * <p></p>{@code messageTime} - time when the message was generated.
 *
 */
public class SocketMessage<T> {

    private final LocalDateTime messageTime;
    private final T payload;

    public SocketMessage(T payload) {
        this.messageTime = LocalDateTime.now();
        this.payload = payload;
    }

    public LocalDateTime getMessageTime() {
        return messageTime;
    }

    public T getPayload() {
        return payload;
    }
}
