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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesBasedDirectoryComparatorTest extends AbstractTestWithTestDir {
    private Path dir1;
    private Path dir2;

    private void setUpDir1And2WithProperties(String propertiesFileName, String propertyName, String value1, String value2) throws IOException {
        dir1 = testDir.resolve("dir1");
        dir2 = testDir.resolve("dir2");
        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        var props1 = new Properties();
        props1.setProperty(propertyName, value1);
        var props2 = new Properties();
        props2.setProperty(propertyName, value2);
        props1.store(Files.newOutputStream(dir1.resolve(propertiesFileName)), null);
        props2.store(Files.newOutputStream(dir2.resolve(propertiesFileName)), null);
    }

    @Test
    public void two_equal_string_should_result_in_0() throws IOException {
        // Given
        var propertiesFileName = "test.properties";
        var propertyName = "testProperty";
        var value = "testValue";
        setUpDir1And2WithProperties(propertiesFileName, propertyName, value, value);

        // When
        var comparator = new PropertiesBasedDirectoryComparator<String>(propertiesFileName, propertyName, String::valueOf);
        int result = comparator.compare(dir1, dir2);

        // Then
        assertThat(result).isEqualTo(0);
    }

    @Test
    public void should_return_negative_if_first_string_comes_first_in_order() throws IOException {
        // Given
        var propertiesFileName = "test.properties";
        var propertyName = "testProperty";
        var value1 = "10";
        var value2 = "9";
        setUpDir1And2WithProperties(propertiesFileName, propertyName, value1, value2);

        // When
        var comparator = new PropertiesBasedDirectoryComparator<>(propertiesFileName, propertyName, String::valueOf);
        int result = comparator.compare(dir1, dir2);

        // Then
        assertThat(result).isNegative();
    }

    @Test
    public void should_return_positive_if_first_is_greater_than_second() throws IOException {
        // Given
        var propertiesFileName = "test.properties";
        var propertyName = "testProperty";
        var value1 = "value2";
        var value2 = "value1";
        setUpDir1And2WithProperties(propertiesFileName, propertyName, value1, value2);

        // When
        var comparator = new PropertiesBasedDirectoryComparator<String>(propertiesFileName, propertyName, String::valueOf);
        int result = comparator.compare(dir1, dir2);

        // Then
        assertThat(result).isPositive();
    }

    @Test
    public void should_convert_property_value_to_integer_and_compare() throws IOException {
        // Given
        var propertiesFileName = "test.properties";
        var propertyName = "testProperty";
        var value1 = "9";
        var value2 = "10";
        setUpDir1And2WithProperties(propertiesFileName, propertyName, value1, value2);

        // When
        var comparator = new PropertiesBasedDirectoryComparator<>(propertiesFileName, propertyName, Integer::valueOf);
        int result = comparator.compare(dir1, dir2);

        // Then
        assertThat(result).isEqualTo(-1);
    }

    @Test
    public void should_compare_timestamps() throws IOException {
        // Given
        var propertiesFileName = "test.properties";
        var propertyName = "creationTime";
        var value1 = "2023-10-01T10:00:00Z"; // later (i.e. greater)
        var value2 = "2023-10-01T09:00:00Z"; // earlier (i.e. lesser)
        setUpDir1And2WithProperties(propertiesFileName, propertyName, value1, value2);

        // When
        var comparator = new PropertiesBasedDirectoryComparator<>(propertiesFileName, propertyName, Instant::parse);
        int result = comparator.compare(dir1, dir2);

        // Then
        assertThat(result).isPositive();
    }

}
