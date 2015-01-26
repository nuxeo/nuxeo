/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.types;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;

import javax.inject.Inject;

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
