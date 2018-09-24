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
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.SchemaNormalization;

import avro.shaded.com.google.common.collect.MapMaker;

/**
 * Very simple SchemaStore that uses a file storage to persists its schemas.
 *
 * @since 10.3
 */
public class FileAvroSchemaStore implements AvroSchemaStore {

    protected static final String AVRO_SCHEMA_EXT = ".avsc";

    protected Path schemaDirectoryPath;

    protected final Map<Long, Schema> schemas = new MapMaker().makeMap();

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
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> Files.isReadable(path) && path.getFileName().toString().endsWith(AVRO_SCHEMA_EXT))
                 .forEach(this::loadSchema);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid base path: " + directory, e);
        }
    }

    /**
     * Load the avro schema from this file.
     */
    public void loadSchema(Path schemaPath) {
        Schema schema;
        try {
            schema = new Schema.Parser().parse(schemaPath.toFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid schema file: " + schemaPath, e);
        }
        addSchema(schema);
    }

    @Override
    public long addSchema(Schema schema) {
        long fp = SchemaNormalization.parsingFingerprint64(schema);
        if (schemas.put(fp, schema) == null) {
            Path schemaPath = schemaDirectoryPath.resolve(
                    String.format("%s-0x%08X%s", schema.getName(), fp, AVRO_SCHEMA_EXT));
            try (PrintWriter out = new PrintWriter(schemaPath.toFile())) {
                out.println(schema.toString(true));
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Cannot write schema to file: " + schemaPath);
            }
        }
        return fp;
    }

    @Override
    public Schema findByFingerprint(long fingerprint) {
        return schemas.get(fingerprint);
    }
}
