/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.runtime.stream.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.avro.Schema;
import org.apache.avro.SchemaNormalization;
import org.apache.avro.message.SchemaStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.lib.stream.codec.AvroSchemaStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.avro.AvroService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.runtime.stream:test-avro-contrib.xml")
public class TestAvroSchemaStore {

    @Inject
    public AvroService service;

    @Test
    public void testSchemaRetrieval() throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("data/avro-schema-example.json")) {
            Schema schema = new Schema.Parser().parse(stream);
            long fingerprint = SchemaNormalization.parsingFingerprint64(schema);
            Schema retrieved = service.getSchemaStore().findByFingerprint(fingerprint);
            assertEquals(schema, retrieved);
        }
    }

    @Test
    public void testSchemaStoreRetrieval() {
        assertNotNull(Framework.getService(AvroService.class).getSchemaStore());
    }

}
