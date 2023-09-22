package ru.turbogoose.cloud.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class FileSizeConverterTest {
    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            0 -> 0b
            1 -> 1b
            999 -> 999b
            1_000 -> 1Kb
            999_999 -> 999Kb
            1_000_000 -> 1Mb
            1_000_000_000 -> 1Gb
            10_000_000_000_000 -> 10000Gb
            """)
    public void test(long bytes, String expectedString) {
        assertThat(FileSizeConverter.toHumanReadableSize(bytes), is(expectedString));
    }
}