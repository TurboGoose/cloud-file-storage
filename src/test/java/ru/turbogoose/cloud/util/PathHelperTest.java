package ru.turbogoose.cloud.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PathHelperTest {
    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            path              -> path
            /path             -> path
            path/             -> path
            /path/to          -> to
            path/to/          -> to
            /path/to/         -> to
            path/to/file.txt  -> file.txt
            /path/to/file.txt -> file.txt
            """)
    public void whenExtractObjectNameThenReturn(String path, String expectedObjectName) {
        assertThat(PathHelper.extractObjectName(path), is(expectedObjectName));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/"})
    public void whenExtractObjectNameThenThrow(String path) {
        assertThrows(IllegalArgumentException.class, () -> PathHelper.extractObjectName(path));
    }

    @Test
    public void assembleBreadcrumbsInclusive() {
        String path = "path/to/folder";
        assertThat(PathHelper.assembleBreadcrumbsFromPath(path), is(Map.of(
                "path", "path",
                "to", "path/to",
                "folder", "path/to/folder"
        )));
    }

    @Test
    public void assembleBreadcrumbsExclusive() {
        String path = "path/to/file.txt";
        assertThat(PathHelper.assembleBreadcrumbsFromPath(path, false), is(Map.of(
                "path", "path",
                "to", "path/to"
        )));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            path/to/file.txt -> path
            """)
    public void extractFirstFolderName(String relativePath, String expectedName) {
        assertThat(PathHelper.extractFirstFolderName(relativePath), is(expectedName));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
                             -> ''
            ''               -> ''
            /                -> ''
            path             -> ?path=path
            path/to          -> ?path=path/to
            path/to/file.txt -> ?path=path/to/file.txt
            """)
    public void getPathParam(String pathValue, String expectedQueryParam) {
        assertThat(PathHelper.getPathParam(pathValue), is(expectedQueryParam));
    }
}