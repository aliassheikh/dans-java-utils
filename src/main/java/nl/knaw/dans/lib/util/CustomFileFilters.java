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
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.nio.file.Path;

public class CustomFileFilters {

    @AllArgsConstructor
    private static class IsSubdirOfFilter extends AbstractFileFilter {
        private final Path parent;

        @Override
        public boolean accept(File file) {
            return file.isDirectory() && file.toPath().getParent().equals(parent);
        }

    }

    @AllArgsConstructor
    private static class IsChildOfFilter extends AbstractFileFilter {
        private final Path parent;

        @Override
        public boolean accept(File file) {
            return file.toPath().getParent().equals(parent);
        }
    }

    public static IOFileFilter subDirectoryOf(Path parent) {
        return new IsSubdirOfFilter(parent);
    }


    public static IOFileFilter childOf(Path parent) {
        return new IsChildOfFilter(parent);
    }
}
