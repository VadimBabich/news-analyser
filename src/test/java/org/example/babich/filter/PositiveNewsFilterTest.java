package org.example.babich.filter;

import org.example.babich.domain.News;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PositiveNewsFilterTest {

    PositiveNewsFilter underTest;

    @BeforeEach
    void setUp() {
        underTest = new PositiveNewsFilter("up", "rise", "good", "success", "high", "über");
    }


    @Test
    void givenPositiveNews_WhenFiltered_ThenShouldReturnTrue() {
        News given = new News(0, "rise fall up good low");

        Assertions.assertTrue(underTest.test(given));
    }

    @Test
    void givenNegativeNews_WhenFiltered_ThenShouldReturnFalse() {
        News given = new News(0, "unter fall up good low");

        Assertions.assertFalse(underTest.test(given));
    }
}