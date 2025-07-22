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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;

public class CreationTimestampComparator implements Comparator<Path> {

    @Override
    public int compare(Path path1, Path path2) {
        try {
            BasicFileAttributes attr1 = Files.readAttributes(path1, BasicFileAttributes.class);
            BasicFileAttributes attr2 = Files.readAttributes(path2, BasicFileAttributes.class);

            return attr1.creationTime().compareTo(attr2.creationTime());
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to read file attributes for comparison", e);
        }
    }
}
