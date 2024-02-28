/*
 * Copyright (C) 2021 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipUtilTest extends AbstractTestWithTestDir {

    @Test
    public void zipDirectory_should_zip_directory_without_root_dir_if_oneRootDir_is_false() throws Exception {
        ZipUtil.zipDirectory(Paths.get("src/test/resources/zip-input/audiences"), testDir.resolve("test.zip"), false);

        assertThat(testDir.resolve("test.zip")).exists();
        // Check that the entries in the zip file are as expected
        ArrayList<String> actualEntries = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(testDir.resolve("test.zip").toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                actualEntries.add(entry.getName());
            }
        }
        assertThat(actualEntries).containsExactlyInAnyOrder(
            "data/",
            "data/a/",
            "data/a/deeper/",
            "data/a/deeper/path/",
            "data/a/deeper/path/With some file.txt",
            "data/random images/",
            "data/random images/image01.png",
            "data/random images/image02.jpeg",
            "data/random images/image03.jpeg",
            "metadata/",
            "metadata/dataset.xml",
            "metadata/files.xml",
            "bag-info.txt",
            "bagit.txt",
            "manifest-sha1.txt",
            "README.md",
            "tagmanifest-sha1.txt"
        );
    }

    @Test
    public void zipDirectory_should_zip_directory_with_root_dir_if_oneRootDir_is_true() throws Exception {
        ZipUtil.zipDirectory(Paths.get("src/test/resources/zip-input/audiences"), testDir.resolve("test.zip"), true);

        assertThat(testDir.resolve("test.zip")).exists();
        // Check that the entries in the zip file are as expected
        ArrayList<String> actualEntries = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(testDir.resolve("test.zip").toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                actualEntries.add(entry.getName());
            }
        }
        assertThat(actualEntries).containsExactlyInAnyOrder(
            "audiences/",
            "audiences/data/",
            "audiences/data/a/",
            "audiences/data/a/deeper/",
            "audiences/data/a/deeper/path/",
            "audiences/data/a/deeper/path/With some file.txt",
            "audiences/data/random images/",
            "audiences/data/random images/image01.png",
            "audiences/data/random images/image02.jpeg",
            "audiences/data/random images/image03.jpeg",
            "audiences/metadata/",
            "audiences/metadata/dataset.xml",
            "audiences/metadata/files.xml",
            "audiences/bag-info.txt",
            "audiences/bagit.txt",
            "audiences/manifest-sha1.txt",
            "audiences/README.md",
            "audiences/tagmanifest-sha1.txt"
        );
    }

}
