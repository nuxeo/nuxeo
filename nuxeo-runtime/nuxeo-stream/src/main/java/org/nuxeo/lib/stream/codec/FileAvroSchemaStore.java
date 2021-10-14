/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.codec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.SchemaNormalization;
import org.apache.avro.SchemaParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Very simple SchemaStore that uses a file storage to persists its schemas.
 *
 * @since 10.3
 */
public class FileAvroSchemaStore implements AvroSchemaStore {

    private static final Log log = LogFactory.getLog(FileAvroSchemaStore.class);

    protected static final String AVRO_SCHEMA_EXT = ".avsc";

    protected final Path schemaDirectoryPath;

    protected final Map<Long, Schema> schemas = new ConcurrentHashMap<>();

    public FileAvroSchemaStore(Path schemaDirectoryPath) {
        this.schemaDirectoryPath = schemaDirectoryPath;
        File directory = schemaDirectoryPath.toFile();
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IllegalArgumentException("Invalid SchemaStore root path: " + schemaDirectoryPath);
            }
            loadSchemas(schemaDirectoryPath);
        } else {
            directory.mkdirs();
        }
    }

    /**
     * Load all avro schema files from this directory. Files must have the .avsc extention.
     */
    public void loadSchemas(Path directory) {
        loadSchemasEndingWith(directory, AVRO_SCHEMA_EXT);
    }

    /**
     * Loads all avro schema files from this directory that end with the pattern.
     *
     * @since 11.5
     */
    protected void loadSchemasEndingWith(Path directory, String pattern) {
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> Files.isReadable(path) && path.getFileName().toString().endsWith(pattern))
                 .forEach(this::loadSchema);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base path: " + directory, e);
        }
    }

    /**
     * Load the avro schema from this file.
     */
    public void loadSchema(Path schemaPath) {
        try {
            Schema schema = new Schema.Parser().parse(schemaPath.toFile());
            addSchema(schema);
        } catch (IOException | SchemaParseException e) {
            log.error("Invalid schema file: " + schemaPath, e);
        }
    }

    @Override
    public long addSchema(Schema schema) {
        long fp = SchemaNormalization.parsingFingerprint64(schema);
        if (schemas.put(fp, schema) == null) {
            Path schemaPath = schemaDirectoryPath.resolve(getFilename(schema.getName(), fp));
            // no need for a lock, even in concurrency the content is the same and file is truncated first
            try (PrintWriter out = new PrintWriter(schemaPath.toFile())) {
                out.println(schema.toString(true));
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Cannot write schema to file: " + schemaPath);
            }
        }
        return fp;
    }

    protected String getFilename(String prefix, long fingerprint) {
        return String.format("%s-0x%08X%s", prefix, fingerprint, AVRO_SCHEMA_EXT);
    }

    @Override
    public Schema findByFingerprint(long fingerprint) {
        Schema ret = schemas.get(fingerprint);
        if (ret != null) {
            return ret;
        }
        // reload from disk, a schema store can be shared
        String suffix = getFilename("", fingerprint);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Fingerprint %d not found, reload from disk: *%s", fingerprint, suffix));
        }
        loadSchemasEndingWith(schemaDirectoryPath, suffix);
        ret = schemas.get(fingerprint);
        if (ret == null) {
            log.warn(String.format("Fingerprint %d not found, no schema matching *%s", fingerprint, suffix));
        }
        return ret;
    }
}
