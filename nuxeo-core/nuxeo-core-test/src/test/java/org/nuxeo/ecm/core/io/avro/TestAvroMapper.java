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

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
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
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = AvroMapperRepositoryInit.class)
public class TestAvroMapper {

    @Inject
    public AvroService service;

    @Inject
    public CoreSession session;

    @Test
    public void testDocumentFull() throws IOException {
        test("/myComplexDocFull");
    }

    @Test
    public void testDocumentPartial() throws IOException {
        test("/myComplexDocPartial");
    }

    protected boolean equals(ArrayProperty e, ArrayProperty o) {
        Object[] v1 = (Object[]) e.getValue();
        Object[] v2 = (Object[]) o.getValue();
        if (v1 == null) {
            return v2 == null;
        }
        for (int i = 0; i < v1.length; i++) {
            if (!Objects.equals(v1[i], v2[i])) {
                return false;
            }
        }
        return true;
    }

    protected boolean equals(BlobProperty e, BlobProperty o) {
        try {
            Blob b1 = (Blob) e.getValue();
            Blob b2 = (Blob) o.getValue();
            return b1.getLength() == b2.getLength()
                    && b1.getString().equals(b2.getString())
                    && b1.getEncoding().equals(b2.getEncoding())
                    && b1.getMimeType().equals(b2.getMimeType());
        } catch (Exception e1) {
            return false;
        }
    }

    protected boolean equals(ComplexProperty e, ComplexProperty o) {
        for (Property p : e.getChildren()) {
            if (!equals(p, o.get(p.getName()))) {
                return false;
            }
        }
        return true;
    }

    protected boolean equals(ListProperty e, ListProperty o) {
        for (int i = 0; i < e.size(); i++) {
            if (!equals(e.get(i), o.get(i))) {
                return false;
            }
        }
        return true;
    }

    protected boolean equals(Property e, Property o) {
        if (e == o) {
            return true;
        }
        if (!(Objects.equals(e.getName(), o.getName()) && e.getClass().equals(o.getClass()))) {
            return false;
        }
        if (e instanceof ArrayProperty) {
            return equals((ArrayProperty) e, (ArrayProperty) o);
        }
        if (e instanceof BlobProperty) {
            return equals((BlobProperty) e, (BlobProperty) o);
        }
        if (e instanceof ComplexProperty) {
            return equals((ComplexProperty) e, (ComplexProperty) o);
        }
        if (e instanceof ListProperty) {
            return equals((ListProperty) e, (ListProperty) o);
        }
        return Objects.equals(e.getValue(), o.getValue());
    }

    protected void test(String path) throws IOException {
        DocumentModel reference = session.getDocument(new PathRef(path));
        // create schema
        Schema avro = service.createSchema(reference);
        // map to avro data model
        GenericRecord record = service.toAvro(avro, reference);
        // try write it
        File file = File.createTempFile(reference.getPathAsString().replaceAll("/", ""), null);
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(avro);
        DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);
        dataFileWriter.create(avro, file);
        dataFileWriter.append(record);
        dataFileWriter.close();
        file.delete();
        // map from avro data model
        DocumentModel mapped = service.fromAvro(record.getSchema(), DocumentModel.class, record);
        // assert reference doc and mapped document are equal
        for (String schema : reference.getSchemas()) {
            for (Property p : reference.getPropertyObjects(schema)) {
                assertTrue(equals(p, mapped.getProperty(p.getXPath())));
            }
        }
    }

}
