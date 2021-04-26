package org.example.babich.converter;

import org.example.babich.domain.News;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

class NewsConverterTest {

    NewsConverter underTest;

    @BeforeEach
    void setUp(){
        underTest = new NewsConverter();
    }

    @Test
    void givenString_WhenConverting_ThenShouldNewsInResponse(){

        String given = "{4}{unter 6 2 10}";
        News news = underTest.apply(ByteBuffer.wrap(given.getBytes()));

        Assertions.assertEquals("unter 6 2 10", news.getHeadline());
    }

}