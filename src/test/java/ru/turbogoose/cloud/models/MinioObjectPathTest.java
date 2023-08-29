package ru.turbogoose.cloud.models;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinioObjectPathTest {

    public static ObjectPath pathOf(String path) {
        return MinioObjectPath.parse(path, 1);
    }

    public static ObjectPath fullPathOf(String fullPath) {
        return MinioObjectPath.parse(fullPath);
    }

    @Nested
    class FactoryMethods {
        @ParameterizedTest
        @CsvSource(delimiterString = "->", textBlock = """
                /                 -> user-1-files/
                /path/            -> user-1-files/path/
                /path/to/         -> user-1-files/path/to/
                /path/to/file.txt -> user-1-files/path/to/file.txt
                """)
        public void parsePath(String path, String expectedFullPath) {
            assertThat(pathOf(path).getFullPath(), is(expectedFullPath));
        }

        @ParameterizedTest
        @NullAndEmptySource
        public void parsingFails(String path) {
            assertThrows(IllegalArgumentException.class, () -> pathOf(path));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "user-1-files/", "user-1-files/path/", "user-1-files/path/to/", "user-1-files/path/to/file.txt"})
        public void parseFullPath(String fullPath) {
            assertThat(fullPathOf(fullPath).getFullPath(), is(fullPath));
        }

        @ParameterizedTest
        @NullAndEmptySource
        public void parsingFullPathFails(String fullPath) {
            assertThrows(IllegalArgumentException.class, () -> fullPathOf(fullPath));
        }
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            /                 -> ''
            /path/            -> path
            /path/to/         -> to
            
            /file.txt         -> file.txt
            /path/to/file.txt -> file.txt
            """)
    public void getObjectName(String path, String expectedObjectName) {
        assertThat(pathOf(path).getObjectName(), is(expectedObjectName));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            /                 -> /
            /path/            -> /path/
            /path/to/         -> /path/to/
            
            /file.txt         -> /file.txt
            /path/to/file.txt -> /path/to/file.txt
            """)
    public void getRelativePath(String path, String expectedPath) {
        assertThat(pathOf(path).getPath(), is(expectedPath));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            /                 -> true
            /path/            -> true
            /path/to/         -> true
            
            /file.txt         -> false
            /path/to/file.txt -> false
            """)
    public void isFolder(String path, boolean isFolder) {
        assertThat(pathOf(path).isFolder(), is(isFolder));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            /,      /any/path,              true
            /path/, /path/to/,             true
            /path/, /path/pic.png,         true
            /path/, /path/to/pic.png,      true

            /path/, /path/,                false
            /path/, /another/path/,        false
            /path/, /,                     false
            
            /path/, /another/path/pic.png, false
            /path/, /pic.png,              false
            """)
    public void isInFolder(String checkedObjectPath, String folderPath, boolean isInFolder) {
        ObjectPath checkedObject = pathOf(checkedObjectPath);
        ObjectPath folder = pathOf(folderPath);
        assertThat(checkedObject.isInFolder(folder), is(isInFolder));
    }

    @Test
    public void isInFolderForFileFails() {
        ObjectPath checkedFile = pathOf("/path/to/file.txt");
        ObjectPath invalidFileObject = pathOf("/path/pic.png");
        assertThrows(IllegalArgumentException.class, () -> checkedFile.isInFolder(invalidFileObject));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            /path/to/folder/,  /,                 /another/,                /another/path/to/folder/
            /path/to/folder/,  /path/,            /road/,                   /road/to/folder/
            /path/to/folder/,  /path/to/,         /location/of/,            /location/of/folder/
            /path/to/folder/,  /path/to/folder/,  /location/of/directory/,  /location/of/directory/
            
            /path/to/file.txt, /,                 /another/,                /another/path/to/file.txt
            /path/to/file.txt, /path/,            /road/,                   /road/to/file.txt
            /path/to/file.txt, /path/to/,         /location/of/,            /location/of/file.txt
            
            /path/to/folder/,  /other/prefix/,    /replacement/,            /path/to/folder/
            /path/to/file.txt, /other/prefix/,    /replacement/,            /path/to/file.txt
            """)
    public void replacePrefix(String path, String prefixToReplace, String replacement, String expectedPath) {
        String actualRelativePath = pathOf(path).replacePrefix(prefixToReplace, replacement).getPath();
        assertThat(actualRelativePath, is(expectedPath));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            /path/,            location,   /location/
            /path/to/,         for,        /path/for/
            /file.txt,         pic.png,    /pic.png
            /path/to/file.txt, pic.png,    /path/to/pic.png
            """)
    public void renameObject(String path, String newName, String expectedPath) {
        String actualRelativePath = pathOf(path).renameObject(newName).getPath();
        assertThat(actualRelativePath, is(expectedPath));
    }

    @Test
    public void renamingRootFolderFails() {
        ObjectPath rootFolder = pathOf("/");
        assertThrows(IllegalStateException.class, () -> rootFolder.renameObject("bebroot"));
    }
}