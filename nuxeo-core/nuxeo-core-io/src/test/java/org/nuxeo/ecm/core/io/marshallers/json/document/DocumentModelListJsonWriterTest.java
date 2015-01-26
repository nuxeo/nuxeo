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

package org.nuxeo.ecm.core.io.marshallers.json.document;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;

@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class DocumentModelListJsonWriterTest extends
        AbstractJsonWriterTest.Local<DocumentModelListJsonWriter, List<DocumentModel>> {

    public DocumentModelListJsonWriterTest() {
        super(DocumentModelListJsonWriter.class, List.class, TypeUtils.parameterize(List.class, DocumentModel.class));
    }

    @Inject
    private CoreSession session;

    public List<DocumentModel> getElements() {
        DocumentModel document1 = session.createDocumentModel("/", "myDoc1", "RefDoc");
        document1 = session.createDocument(document1);
        DocumentModel document2 = session.createDocumentModel("/", "myDoc2", "RefDoc");
        document2 = session.createDocument(document2);
        DocumentModel document3 = session.createDocumentModel("/", "myDoc3", "RefDoc");
        document3 = session.createDocument(document3);
        return Arrays.asList(document1, document2, document3);
    }

    @Test
    public void test() throws Exception {
        List<DocumentModel> elements = getElements();
        JsonAssert json = jsonAssert(elements);
        json.isObject();
        json.properties(2);
        json.has("entity-type").isEquals("documents");
        json = json.has("entries").length(elements.size());
        json.childrenContains("entity-type", "document", "document", "document");
        json.childrenContains("title", "myDoc1", "myDoc2", "myDoc3");
    }

}
