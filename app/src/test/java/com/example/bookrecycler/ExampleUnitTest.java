package com.example.bookrecycler;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void timePassedTest() {
        CharSequence timePassed = Utils.getTimePassed(new Date().getTime());
        assertNotNull(timePassed);
    }
}