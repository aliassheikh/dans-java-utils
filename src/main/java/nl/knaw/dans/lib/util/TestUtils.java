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

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestUtils {
    public static void assertDirectoriesEqual(Path expected, Path actual) throws IOException {
        Set<Path> expectedFiles;
        try (Stream<Path> stream = Files.walk(expected)) {
            expectedFiles = stream.collect(Collectors.toSet());
        }

        Set<Path> actualFiles;
        try (Stream<Path> stream = Files.walk(actual)) {
            actualFiles = stream.collect(Collectors.toSet());
        }

        Assertions.assertEquals(expectedFiles.size(), actualFiles.size(), "Directories do not have the same number of files/directories");

        for (Path expectedFile : expectedFiles) {
            Path relative = expected.relativize(expectedFile);
            Path actualFile = actual.resolve(relative);

            Assertions.assertTrue(Files.exists(actualFile), "Expected file does not exist in actual directory: " + relative);

            if (Files.isRegularFile(expectedFile)) {
                Assertions.assertArrayEquals(Files.readAllBytes(expectedFile), Files.readAllBytes(actualFile), "File contents differ for file: " + relative);
            }
        }
    }
}