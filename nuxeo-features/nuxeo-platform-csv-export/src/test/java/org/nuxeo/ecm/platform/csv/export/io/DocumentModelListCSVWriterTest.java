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

package org.nuxeo.ecm.platform.csv.export.io;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriterTest;
import org.nuxeo.ecm.core.io.marshallers.csv.CSVAssert;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.3
 */
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.default.config")
@Deploy("org.nuxeo.ecm.platform.csv.export")
public class DocumentModelListCSVWriterTest
        extends AbstractCSVWriterTest.Local<DocumentModelListCSVWriter, List<DocumentModel>> {

    public DocumentModelListCSVWriterTest() {
        super(DocumentModelListCSVWriter.class, List.class, TypeUtils.parameterize(List.class, DocumentModel.class));
    }

    @Inject
    protected CoreSession session;

    public List<DocumentModel> getElements() {
        DocumentModel document1 = session.createDocumentModel("/", "myDoc1", "File");
        document1 = session.createDocument(document1);
        DocumentModel document2 = session.createDocumentModel("/", "myDoc2", "File");
        document2 = session.createDocument(document2);
        DocumentModel document3 = session.createDocumentModel("/", "myDoc3", "File");
        document3 = session.createDocument(document3);
        return Arrays.asList(document1, document2, document3);
    }

    @Test
    public void test() throws Exception {
        List<DocumentModel> elements = getElements();
        CSVAssert csv = csvAssert(elements);
        csv.length(elements.size());
        csv.childrenContains("title", "myDoc1", "myDoc2", "myDoc3");
    }
}
