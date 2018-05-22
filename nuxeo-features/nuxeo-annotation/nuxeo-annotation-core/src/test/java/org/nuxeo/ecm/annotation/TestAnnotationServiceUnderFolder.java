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

package org.nuxeo.ecm.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 10.2
 */
@Deploy("org.nuxeo.annotation.core.test:test-annotation-properties-contrib.xml")
public class TestAnnotationServiceUnderFolder extends TestAnnotationService {

    @Test
    public void testAnnotationFolderCreation() {

        DocumentModel domain = session.createDocumentModel("/", "test", "Domain");
        session.createDocument(domain);

        DocumentModel docToAnnotate = session.createDocumentModel("/test", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String annotationId = "foo";
        String docIdToAnnotate = docToAnnotate.getId();
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = new AnnotationImpl();
        annotation.setId(annotationId);
        annotation.setDocumentId(docIdToAnnotate);
        annotation.setXpath(xpathToAnnotate);
        annotationService.createAnnotation(session, annotation);
        session.save();

        DocumentModel folder = session.getDocument(new PathRef("/test/Annotations"));
        assertNotNull(folder);
        assertEquals("HiddenFolder", folder.getType());
        assertEquals(1, session.getChildren(folder.getRef()).size());

    }

}
