/*
 * (C) Copyright 2016-2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper to load data from a CSV file.
 * <p>
 * The actual consumer of rows is a parameter passed by the caller.
 *
 * @since 8.4
 */
public class DirectoryCSVLoader {

    private static final Logger log = LogManager.getLogger(DirectoryCSVLoader.class);

    /**
     * The special CSV value ({@value}) used to denote that a {@code null} should be used for a value.
     */
    public static final String CSV_NULL_MARKER = "__NULL__";

    private DirectoryCSVLoader() {
    }

    /**
     * Loads the CSV data file based on the provided schema, and creates the corresponding entries using the provided
     * loader.
     *
     * @param dataFileName the file name containing CSV data
     * @param delimiter the CSV column separator
     * @param schema the data schema
     * @param loader the actual consumer of loaded rows
     * @since 8.4
     * @see #loadData(Blob, char, Schema, Consumer)
     */
    public static void loadData(String dataFileName, char delimiter, Schema schema,
            Consumer<Map<String, Object>> loader) {
        Blob blob = createBlob(dataFileName);
        loadData(blob, delimiter, schema, loader);
    }

    protected static Blob createBlob(String dataFileName) {
        try (InputStream in = getResource(dataFileName)) {
            return Blobs.createBlob(in, "text/csv");
        } catch (IOException e) {
            throw new DirectoryException("Read error while creating blob from data file: " + dataFileName, e);
        }
    }

    /**
     * Loads the CSV data file based on the provided schema, and creates the corresponding entries using the provided
     * loader.
     *
     * @param dataBlob the blob containing CSV data
     * @param delimiter the CSV column separator
     * @param schema the data schema
     * @param loader the actual consumer of loaded rows
     * @since 11.1
     */
    public static void loadData(Blob dataBlob, char delimiter, Schema schema, Consumer<Map<String, Object>> loader) {
        BiConsumer<Map<String, Object>, Integer> loaderWithLineno = toLoaderEnrichedOnError(loader);
        String dataFileName = dataBlob.getFilename();
        try (InputStream in = dataBlob.getStream();
                CSVParser csvParser = new CSVParser(new InputStreamReader(in, UTF_8),
                        CSVFormat.DEFAULT.withDelimiter(delimiter).withHeader())) {

            Map<String, Integer> header = csvParser.getHeaderMap();

            List<Field> fields = new ArrayList<>();
            for (String columnName : header.keySet()) {
                Field field = schema.getField(columnName.trim());
                if (field == null) {
                    throw new DirectoryException("Column not found: " + columnName + " in schema: " + schema.getName());
                }
                fields.add(field);
            }

            int lineno = 1; // header was first line
            for (CSVRecord record : csvParser) {
                lineno++;
                if (record.size() == 0 || record.size() == 1 && StringUtils.isBlank(record.get(0))) {
                    // NXP-2538: allow columns with only one value but skip empty lines
                    continue;
                }
                if (!record.isConsistent()) {
                    log.error("Invalid column count while reading CSV file: {}, line: {}", dataFileName, lineno);
                    continue;
                }

                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < header.size(); i++) {
                    Field field = fields.get(i);
                    String value = record.get(i);
                    Object v = CSV_NULL_MARKER.equals(value) ? null : decode(field, value);
                    map.put(field.getName().getPrefixedName(), v);
                }
                loaderWithLineno.accept(map, lineno);
            }
        } catch (IOException e) {
            throw new DirectoryException("Read error while reading data file: " + dataFileName, e);
        }
    }

    protected static BiConsumer<Map<String, Object>, Integer> toLoaderEnrichedOnError(
            Consumer<Map<String, Object>> loader) {
        return (map, lineno) -> {
            try {
                loader.accept(map);
            } catch (DirectoryException e) {
                e.addInfo("At line: " + lineno);
                throw e;
            }
        };
    }

    protected static Object decode(Field field, String value) {
        Type type = field.getType();
        if (type instanceof DateType) {
            // compat with earlier code, interpret in the local timezone and not UTC
            Calendar cal = new GregorianCalendar();
            cal.setTime(Timestamp.valueOf(value));
            return cal;
        } else {
            return type.decode(value);
        }
    }

    @SuppressWarnings("resource")
    protected static InputStream getResource(String name) {
        InputStream in = DirectoryCSVLoader.class.getClassLoader().getResourceAsStream(name);
        if (in == null) {
            in = Framework.getResourceLoader().getResourceAsStream(name);
            if (in == null) {
                throw new DirectoryException("Data file not found: " + name);
            }
        }
        return in;
    }

}
