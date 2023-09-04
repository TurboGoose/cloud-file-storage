package ru.turbogoose.cloud.repositories.minio;

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

    public static MinioObjectPath pathOf(String path) {
        return MinioObjectPath.compose(1, path);
    }

    public static MinioObjectPath fullPathOf(String fullPath) {
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
        @ValueSource(strings = "wrong-home-folder-format")
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
    public void getPath(String path, String expectedPath) {
        assertThat(pathOf(path).getPath(), is(expectedPath));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            /path/             -> /
            /path/to/          -> /path/
            /path/to/file.txt  -> /path/to/
            """)
    public void getParent(String path, String expectedParentPath) {
        assertThat(pathOf(path).getParent().getPath(), is(expectedParentPath));
    }

    @Test
    public void getParentForRootFolderFails() {
        MinioObjectPath root = pathOf("/");
        assertThrows(UnsupportedOperationException.class, root::getParent);
    }


    @Test
    public void getRootFolder() {
        assertThat(MinioObjectPath.getRootFolder(1).getFullPath(), is("user-1-files/"));
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
            /,      /any/path/,            true
            /path/, /path/to/,             true
            /path/, /path/pic.png,         true
            /path/, /path/to/pic.png,      true
            /path/, /path/,                true
            
            /path/, /another/path/,        false
            /path/, /,                     false
            
            /path/, /another/path/pic.png, false
            /path/, /pic.png,              false
            """)
    public void isInFolder(String folderPath, String checkedObjectPath, boolean isInFolder) {
        MinioObjectPath checkedObject = pathOf(checkedObjectPath);
        MinioObjectPath folder = pathOf(folderPath);
        assertThat(checkedObject.isInFolder(folder), is(isInFolder));
    }

    @Test
    public void isInFolderForFileFails() {
        MinioObjectPath checkedFile = pathOf("/path/to/file.txt");
        MinioObjectPath invalidFileObject = pathOf("/path/pic.png");
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
        String actualPath = pathOf(path).replacePrefix(prefixToReplace, replacement).getPath();
        assertThat(actualPath, is(expectedPath));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            /,        path/,     /path/
            /,        file.txt,  /file.txt
            /path/,   '',        /path/
            /path/,   to/,       /path/to/
            /path/,   file.txt,  /path/file.txt
            /path/,   to/folder/, /path/to/folder/
            """)
    public void resolveObjectToFolder(String folderPath, String resolvedObject, String expectedPath) {
        String actualPath = pathOf(folderPath).resolve(resolvedObject).getPath();
        assertThat(actualPath, is(expectedPath));
    }

    @Test
    public void resolveNullObjectToFolderFails() {
        MinioObjectPath folderPath = pathOf("/path/");
        assertThrows(IllegalArgumentException.class, () -> folderPath.resolve(null));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            /file.txt,        ''
            /file.txt,        folder/
            /file.txt,        pic.png
            /path/file.txt,   folder/
            /path/file.txt,   pic.png
            """)
    public void resolveObjectToFileFails(String filePath, String resolvedObject) {
        assertThrows(UnsupportedOperationException.class, () -> pathOf(filePath).resolve(resolvedObject));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            /path/,            location,   /location/
            /path/to/,         for,        /path/for/
            /file.txt,         pic.png,    /pic.png
            /path/to/file.txt, pic.png,    /path/to/pic.png
            """)
    public void renameObject(String path, String newName, String expectedPath) {
        String actualPath = pathOf(path).renameObject(newName).getPath();
        assertThat(actualPath, is(expectedPath));
    }

    @Test
    public void renamingRootFolderFails() {
        MinioObjectPath rootFolder = pathOf("/");
        assertThrows(UnsupportedOperationException.class, () -> rootFolder.renameObject("bebroot"));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            /                 -> true
            /path/            -> false
            /path/to/         -> false
            /file.txt         -> false
            /path/to/file.txt -> false
            """)
    public void isRootFolder(String path, boolean isRootFolder) {
        assertThat(pathOf(path).isRootFolder(), is(isRootFolder));
    }
}