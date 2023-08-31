package ru.turbogoose.cloud.mappers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.turbogoose.cloud.util.ObjectPathMapper;

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
    public void convertFolderPathFromUrlParam(String inputFolderPath, String expectedPath) {
        assertThat(ObjectPathMapper.fromUrlParam(inputFolderPath), is(expectedPath));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            file.txt        -> /file.txt
            path/file.txt   -> /path/file.txt
            """)
    public void convertFilePathFromUrlParam(String inputFilePath, String expectedPath) {
        assertThat(ObjectPathMapper.fromUrlParam(inputFilePath, true), is(expectedPath));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
                           -> ''
            ''             -> ''
            /              -> /
            /path/         -> path
            /path/to/      -> path/to
            /file.txt      -> file.txt
            /path/file.txt -> path/file.txt
            """)
    public void convertToUrlParam(String inputPath, String expectedPath) {
        assertThat(ObjectPathMapper.toUrlParam(inputPath), is(expectedPath));
    }
}