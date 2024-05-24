package ru.job4j.grabber.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class HabrCareerDateTimeParserTest {

    @Test
    public void whenParseDateTimeIsCorrect() {
        String text = "2024-05-17T18:27:06+03:00";
        LocalDateTime result = new HabrCareerDateTimeParser().parse(text);
        assertThat(result).isEqualTo("2024-05-17T18:27:06");
    }

    @Test
    public void whenParseDateTimeIsCorrect2() {
        String text = "2024-05-16T19:50:31+03:00";
        LocalDateTime result = new HabrCareerDateTimeParser().parse(text);
        assertThat(result).isEqualTo("2024-05-16T19:50:31");
    }
}