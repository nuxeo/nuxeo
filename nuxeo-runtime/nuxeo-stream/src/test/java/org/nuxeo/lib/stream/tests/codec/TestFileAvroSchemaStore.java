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
package org.nuxeo.lib.stream.tests.codec;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.avro.reflect.ReflectData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.AvroSchemaStore;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.codec.FileAvroSchemaStore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 10.3
 */
public class TestFileAvroSchemaStore {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testAvroEvolution() throws IOException {
        Path root = folder.newFolder().toPath();
        AvroSchemaStore store = new FileAvroSchemaStore(root);

        long v1fp = store.addSchema(ReflectData.get().getSchema(MessageV1.class));
        assertTrue(v1fp != 0);
        long v2fp = store.addSchema(ReflectData.get().getSchema(MessageV2.class));
        assertTrue(v1fp != v2fp);

        // open another store using the same storage directory
        AvroSchemaStore store2 = new FileAvroSchemaStore(root);
        assertNotNull(store2.findByFingerprint(v1fp));
        assertNotNull(store2.findByFingerprint(v2fp));
    }
}
