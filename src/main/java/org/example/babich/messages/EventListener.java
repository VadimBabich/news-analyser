package org.example.babich.messages;

/**
 * used as a contract to subscribe to an incoming socket message.
 * @param <T> event payload
 */
public interface EventListener<T> {
    void onPositiveNews(SocketMessage<T> message);
}
