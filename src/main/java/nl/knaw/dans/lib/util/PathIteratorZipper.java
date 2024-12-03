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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Zips files from an iterator of paths into a zip file up to a maximum number of files and bytes (the first limit reached). The resulting ZIP file can be compressed or not. The files in the ZIP file
 * can be renamed. The ZIP file can be overwritten if it already exists.
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PathIteratorZipper {
    /**
     * The root directory of the files to zip.
     */
    @NonNull
    private final Path rootDir;
    /**
     * The iterator of paths to the files to zip.
     */
    @NonNull
    private final Iterator<Path> sourceIterator;
    /**
     * The path to the target zip file.
     */
    @NonNull
    private final Path targetZipFile;
    /**
     * Whether to overwrite the target zip file if it already exists.
     */
    @Builder.Default
    private final boolean overwrite = true;
    /**
     * Whether to compress the files in the target zip file.
     */
    @Builder.Default
    private final boolean compress = false;
    /**
     * The maximum number of files to include in the target zip file.
     */
    @Builder.Default
    private final int maxNumberOfFiles = Integer.MAX_VALUE;
    /**
     * The maximum number of bytes to include in the target zip file.
     */
    @Builder.Default
    private final long maxNumberOfBytes = 1073741824; // 1 GB
    /**
     * A map of source file paths to target file paths (relative to the root directory, including the file name). If a source file path is not in the map, the file is zipped with its original file
     * path.
     */
    @Builder.Default
    private final Map<String, String> renameMap = new HashMap<>();

    /**
     * Zips files from the source iterator into the target zip file.
     *
     * @return the path to the target zip file.
     * @throws IOException if the target zip file already exists and overwrite is false, or if an I/O error occurs while zipping the files.
     */
    public Path zip() throws IOException {
        if (overwrite && Files.exists(targetZipFile)) {
            Files.delete(targetZipFile);
        }
        else {
            if (Files.exists(targetZipFile)) {
                throw new IOException("Target zip file already exists: " + targetZipFile);
            }
        }

        try (OutputStream outputStream = Files.newOutputStream(targetZipFile)) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            try (ZipArchiveOutputStream zipArchiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream)) {
                int numberOfFilesAdded = 0;
                long numberOfBytesAdded = 0;
                while (sourceIterator.hasNext() && numberOfFilesAdded < maxNumberOfFiles && numberOfBytesAdded < maxNumberOfBytes) {
                    Path path = sourceIterator.next();
                    if (Files.isRegularFile(path)) {
                        try {
                            addFileToZipStream(zipArchiveOutputStream, path);
                            numberOfFilesAdded++;
                            numberOfBytesAdded += Files.size(path);
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return targetZipFile;
        }
    }

    private void addFileToZipStream(ZipArchiveOutputStream zipArchiveOutputStream, Path fileToZip) throws IOException {
        if (!fileToZip.startsWith(rootDir)) {
            throw new IllegalArgumentException("File to zip is not a descendant of root directory: " + fileToZip);
        }
        String entryName = rootDir.relativize(fileToZip).toString();
        if (renameMap.containsKey(entryName)) {
            entryName = renameMap.get(entryName);
        }
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(fileToZip, entryName);
        zipArchiveEntry.setMethod(compress ? ZipArchiveEntry.STORED : ZipArchiveEntry.DEFLATED);
        zipArchiveOutputStream.putArchiveEntry(zipArchiveEntry);
        try (InputStream fileInputStream = Files.newInputStream(fileToZip)) {
            IOUtils.copy(fileInputStream, zipArchiveOutputStream);
            zipArchiveOutputStream.closeArchiveEntry();
        }
    }
}