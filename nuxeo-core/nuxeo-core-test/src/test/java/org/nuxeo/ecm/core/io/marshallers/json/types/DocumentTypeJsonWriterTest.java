/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.types;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;

@Features(CoreFeature.class)
public class DocumentTypeJsonWriterTest extends AbstractJsonWriterTest.Local<DocumentTypeJsonWriter, DocumentType> {

    public DocumentTypeJsonWriterTest() {
        super(DocumentTypeJsonWriter.class, DocumentType.class);
    }

    @Inject
    private SchemaManager schemaManager;

    @Test
    public void test() throws Exception {
        DocumentType type = schemaManager.getDocumentType("Folder");
        JsonAssert json = jsonAssert(type);
        json.properties(5);
        json.has("entity-type").isEquals("docType");
        json.has("name").isEquals("Folder");
        json.has("parent").isEquals("Document");
        json.has("facets").contains("Folderish");
        json = json.has("schemas").length(2);
        json.childrenContains("entity-type", "schema", "schema");
        json.childrenContains("name", "common", "dublincore");
    }

    @Test
    public void testWithoutParent() throws Exception {
        DocumentType type = schemaManager.getDocumentType("Document");
        JsonAssert json = jsonAssert(type);
        json.properties(5);
        json.has("parent").isNull();
    }

}
