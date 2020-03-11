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

package org.nuxeo.ecm.core.io.marshallers.json.document;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
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
