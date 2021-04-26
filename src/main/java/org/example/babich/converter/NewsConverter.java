package org.example.babich.converter;

import org.example.babich.domain.News;

import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * convert socket data to domain object
 */
public class NewsConverter implements Function<ByteBuffer, News> {

    final Pattern pattern = Pattern.compile("\\{(?<priority>[0-9])}" +
                    "\\{(?<headline>[ a-zA-Z0-9\\u00E4\\u00F6\\u00FC\\u00C4\\u00D6\\u00DC\\u00df]{5,100})}");

    @Override
    public News apply(ByteBuffer byteBuffer) {
        Matcher matcher = pattern.matcher(new String(byteBuffer.array()));
        if(!matcher.find()){
            throw new IllegalArgumentException("Wrong message format '" + new String(byteBuffer.array()) + "'");
        }

        return new News(Integer.parseInt(matcher.group("priority"))
                , matcher.group("headline"));
    }
}
