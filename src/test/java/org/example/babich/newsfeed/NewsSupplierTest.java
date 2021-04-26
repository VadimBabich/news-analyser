package org.example.babich.newsfeed;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewsSupplierTest {

    NewsSupplier underTest;
    int distributionSize = 100;
    int priorityCount = 10;

    int[] expectedLinearDisproportion = new int[]{5, 11, 18, 26, 35, 45, 56, 68, 81, 100};

    @BeforeEach
    void setUp(){
        underTest = new NewsSupplier(new String[]{});
    }

    @Test
    void givenLinearDisproportion_WhenGeneratingIntervals_ShouldReturnDisproportionValuesArray(){
        int[] values = NewsSupplier.generateLinearDisproportion(distributionSize, priorityCount);

        Assertions.assertArrayEquals(expectedLinearDisproportion, values);
    }

    @Test
    void generatePriority(){
        int[] values = NewsSupplier.getPriorityDistributionMap(distributionSize, priorityCount, expectedLinearDisproportion);
        Assertions.assertEquals(9, values[0]);
        Assertions.assertEquals(9, values[5]);

        Assertions.assertEquals(8, values[6]);
        Assertions.assertEquals(8, values[11]);

        Assertions.assertEquals(7, values[12]);
        Assertions.assertEquals(7, values[18]);

        Assertions.assertEquals(6, values[19]);
        Assertions.assertEquals(6, values[26]);

        Assertions.assertEquals(5, values[27]);
        Assertions.assertEquals(5, values[35]);

        Assertions.assertEquals(4, values[36]);
        Assertions.assertEquals(4, values[45]);

        Assertions.assertEquals(3, values[46]);
        Assertions.assertEquals(3, values[56]);

        Assertions.assertEquals(2, values[57]);
        Assertions.assertEquals(2, values[68]);

        Assertions.assertEquals(1, values[69]);
        Assertions.assertEquals(1, values[81]);

        Assertions.assertEquals(0, values[82]);
        Assertions.assertEquals(0, values[99]);
    }
}