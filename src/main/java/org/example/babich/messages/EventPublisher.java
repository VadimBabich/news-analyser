package org.example.babich.messages;

public interface EventPublisher<T> {

    void addEventListener(EventListener<T> eventListener);
}
