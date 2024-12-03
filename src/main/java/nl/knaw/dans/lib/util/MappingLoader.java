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

import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a mapping from a CSV file.
 */
@Builder
public class MappingLoader {
    /**
     * The path to the CSV file to load the mapping from.
     */
    @NonNull
    private final Path csvFile;
    /**
     * The name of the column in the CSV file that contains the keys.
     */
    @NonNull
    private final String keyColumn;
    /**
     * The name of the column in the CSV file that contains the values.
     */
    @NonNull
    private final String valueColumn;

    public Map<String, String> load() throws IOException {
        try (CSVParser parser = CSVParser.parse(csvFile.toFile(), StandardCharsets.UTF_8, CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build())) {
            HashMap<String, String> result = new HashMap<>();

            for (CSVRecord record : parser) {
                result.put(record.get(keyColumn), record.get(valueColumn));
            }

            return result;
        }
    }
}
