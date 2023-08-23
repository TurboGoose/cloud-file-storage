package ru.turbogoose.cloud.models;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MinioObjectTest {

    @Test
    void fromUrlParam() {
        assertThat(MinioObject.fromUrlParam(null, 1), is(new MinioObject("user-1-files/")));
        assertThat(MinioObject.fromUrlParam("", 1), is(new MinioObject("user-1-files/")));
        assertThat(MinioObject.fromUrlParam("path/to/folder", 1),
                is(new MinioObject("user-1-files/path/to/folder/")));
    }

    @Test
    void getObjectName() {
        assertThat(new MinioObject("user-1-files/path/to/folder/").getObjectName(), is("folder"));
        assertThat(new MinioObject("user-1-files/path/to/folder/pic.png").getObjectName(), is("pic.png"));
    }

    @Test
    void toUrlParam() {
        assertThat(new MinioObject("user-1-files/path/to/folder/").toUrlParam(),
                is("path/to/folder"));
        assertThat(new MinioObject("user-1-files/path/to/folder/pic.png").toUrlParam(),
                is("path/to/folder/pic.png"));
    }

    @Test
    void isFolder() {
        assertThat(new MinioObject("user-1-files/path/to/folder/").isFolder(), is(true));
        assertThat(new MinioObject("user-1-files/path/to/folder/pic.png").isFolder(), is(false));
    }
}