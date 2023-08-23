package ru.turbogoose.cloud.models;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MinioObjectPathTest {
    @Test
    void testParseAbstractFolderFactoryMethod() {
        assertThat(MinioObjectPath.parseAbstractFolder("", 1).getAbsolutePath(), is("user-1-files/"));
        assertThat(MinioObjectPath.parseAbstractFolder("/", 1).getAbsolutePath(), is("user-1-files/"));
        assertThat(MinioObjectPath.parseAbstractFolder("path", 1).getAbsolutePath(), is("user-1-files/path/"));
        assertThat(MinioObjectPath.parseAbstractFolder("path/to", 1).getAbsolutePath(), is("user-1-files/path/to/"));
    }

    @Test
    void testParseAbsoluteFactoryMethod() {
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/").getAbsolutePath(), is("user-1-files/"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/").getAbsolutePath(), is("user-1-files/path/"));
    }

    @Test
    void testGetObjectName() {
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/").getObjectName(), is("/"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/").getObjectName(), is("path"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/to").getObjectName(), is("to"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/pic.png").getObjectName(), is("pic.png"));
    }

    @Test
    void testGetAbsolutePath() {
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/").getAbsolutePath(), is("user-1-files/"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/").getAbsolutePath(), is("user-1-files/path/"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/to").getAbsolutePath(), is("user-1-files/path/to"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/pic.png").getAbsolutePath(), is("user-1-files/path/pic.png"));
    }

    @Test
    void testGetAbstractPath() {
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/").getAbstractPath(), is("/"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/").getAbstractPath(), is("path"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/to").getAbstractPath(), is("path/to"));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/pic.png").getAbstractPath(), is("path/pic.png"));
    }

    @Test
    void testIsFolder() {
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/").isFolder(), is(true));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/").isFolder(), is(true));
        assertThat(MinioObjectPath.parseAbsolute("user-1-files/path/pic.png").isFolder(), is(false));
    }
}