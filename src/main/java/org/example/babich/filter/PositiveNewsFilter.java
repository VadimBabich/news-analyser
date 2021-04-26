package org.example.babich.filter;

import org.example.babich.domain.News;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * it is filter of positive news -
 * If more than 50% of words in the headline are positive  the news item as a whole is considered positive.
 */
public class PositiveNewsFilter implements Predicate<News> {

    private final Set<String> positiveWords;

    public PositiveNewsFilter(String... positiveWords) {
        this.positiveWords = Arrays.stream(positiveWords).map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean test(News news) {

        String[] words = news.getHeadline().split("\\W+");
        long countPositiveWords = Arrays.stream(words)
                .map(String::toLowerCase)
                .filter(positiveWords::contains)
                .count();

        return countPositiveWords > (words.length - countPositiveWords);
    }
}
