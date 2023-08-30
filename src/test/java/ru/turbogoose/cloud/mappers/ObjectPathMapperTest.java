package ru.turbogoose.cloud.mappers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ObjectPathMapperTest {
    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
                    -> /
            ''      -> /
            /       -> /
            path    -> /path/
            path/to -> /path/to/
            """)
    public void fromUrlParam(String inputPath, String expectedPath) {
        assertThat(ObjectPathMapper.fromUrlParam(inputPath), is(expectedPath));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
                      -> ''
            ''        -> ''
            /         -> /
            /path/    -> path
            /path/to/ -> path/to
            """)
    public void toUrlParam(String inputPath, String expectedPath) {
        assertThat(ObjectPathMapper.toUrlParam(inputPath), is(expectedPath));
    }
}