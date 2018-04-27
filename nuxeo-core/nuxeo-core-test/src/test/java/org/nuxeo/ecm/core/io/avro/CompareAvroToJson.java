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
package org.nuxeo.ecm.core.io.avro;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.message.BinaryMessageEncoder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.avro.AvroService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = AvroMapperRepositoryInit.class)
public class CompareAvroToJson {

    @Inject
    public AvroService service;

    @Inject
    public CoreSession session;

    @Inject
    public MarshallerRegistry registry;

    @Test
    public void testDocumentFull() throws IOException {
        DocumentModel reference = session.getDocument(new PathRef("/myComplexDocFull"));
        byte[] avro = asAvro(reference);
        byte[] json = asJson(reference);
        assertTrue(avro.length < json.length);
    }

    protected byte[] asAvro(DocumentModel doc) throws IOException {
        Schema schema = service.createSchema(doc);
        GenericRecord record = service.toAvro(schema, doc);
        BinaryMessageEncoder<GenericRecord> encoder = new BinaryMessageEncoder<>(GenericData.get(), schema);
        return encoder.encode(record).array();
    }

    protected byte[] asJson(DocumentModel doc) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RenderingContext ctx = RenderingContext.CtxBuilder.properties("*").get();
        Writer<DocumentModel> writer = registry.getWriter(ctx, DocumentModel.class, MediaType.APPLICATION_JSON_TYPE);
        writer.write(doc, DocumentModel.class, DocumentModel.class, MediaType.APPLICATION_JSON_TYPE, baos);
        return baos.toByteArray();
    }

}
