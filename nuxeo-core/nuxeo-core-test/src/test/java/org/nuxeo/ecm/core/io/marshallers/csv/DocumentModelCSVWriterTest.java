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
 *     Funsho David
 */

package org.nuxeo.ecm.core.io.marshallers.csv;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.3
 */
@Features(CoreFeature.class)
public class DocumentModelCSVWriterTest extends AbstractCSVWriterTest.Local<DocumentModelCSVWriter, DocumentModel> {

    protected DocumentModel document;

    @Inject
    protected CoreSession session;

    public DocumentModelCSVWriterTest() {
        super(DocumentModelCSVWriter.class, DocumentModel.class);
    }

    @Before
    public void setup() {
        document = session.createDocumentModel("/", "myDoc", "File");
        document.setPropertyValue("dc:description", "There is a , in the description");
        document = session.createDocument(document);
    }

    @Test
    public void testDefault() throws Exception {
        CSVAssert csv = csvAssert(document);
        csv.has("repository").isEquals("test");
        csv.has("uid").isEquals(document.getId());
        csv.has("path").isEquals("/myDoc");
        csv.has("type").isEquals("File");
        csv.has("state").isEquals("project");
        csv.has("parentRef").isEquals(document.getParentRef().toString());
        csv.has("isCheckedOut").isTrue();
        csv.has("isVersion").isFalse();
        csv.has("isProxy").isFalse();
        csv.has("isTrashed").isFalse();
        csv.has("changeToken").isNull();
        csv.has("title").isEquals("myDoc");
        csv.has("dc:description").isEquals("There is a , in the description");
    }
}
