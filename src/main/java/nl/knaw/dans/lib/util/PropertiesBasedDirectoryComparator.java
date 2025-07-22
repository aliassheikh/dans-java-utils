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

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import java.util.function.Function;

@AllArgsConstructor
public class PropertiesBasedDirectoryComparator<T extends Comparable<T>> implements Comparator<Path> {

    private final String propertiesFileName;
    private final String propertyName;
    private final Function<String, T> valueConverter;

    @Override
    public int compare(Path dir1, Path dir2) {
        T value1 = getPropertyValue(dir1);
        T value2 = getPropertyValue(dir2);

        if (value1 == null && value2 == null) {
            return 0;
        }
        else if (value1 == null) {
            return -1; // nulls are considered less than non-null values
        }
        else if (value2 == null) {
            return 1; // non-null values are considered greater than nulls
        }
        return value1.compareTo(value2);

    }

    private T getPropertyValue(Path path) {
        var propsFile = path.resolve(propertiesFileName);
        var props = new Properties();
        try {
            props.load(Files.newInputStream(propsFile));
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load properties from " + propsFile, e);
        }
        return valueConverter.apply(props.getProperty(propertyName));
    }
}
