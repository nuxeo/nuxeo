/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.bulk.message;

import static org.junit.Assert.assertEquals;

import org.apache.avro.SchemaFormatter;
import org.apache.avro.SchemaNormalization;
import org.apache.avro.reflect.ReflectData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.test.ModuleUnderTest;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.18
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestBulkCommand {

    // the test will fail when changing the BulkCommand object
    // this is expected as a remember to contribute BulkCommand schemas
    // when it happens, upgrade the test fingerprint & schema then copy the test schema file to sources, replace the
    // -test suffix by the fingerprint and contribute it to Nuxeo
    @Test
    public void testBulkCommandAvroSchema() {
        var avroSchema = ReflectData.get().getSchema(BulkCommand.class);
        // first check fingerprint like FileAvroSchemaStore is computing it
        var fingerprint = SchemaNormalization.parsingFingerprint64(avroSchema);
        var formattedFingerPrint = "0x%08X".formatted(fingerprint);
        assertEquals("0xEBC575C72508AD0B", formattedFingerPrint);
        // then check schema
        var actualCommandSchema = SchemaFormatter.format("json/pretty", avroSchema);
        var expectedCommandSchema = ModuleUnderTest.getClassLoaderResourceAsString("avro/BulkCommand-test.avsc");
        assertEquals(expectedCommandSchema, actualCommandSchema + System.lineSeparator());
    }
}
