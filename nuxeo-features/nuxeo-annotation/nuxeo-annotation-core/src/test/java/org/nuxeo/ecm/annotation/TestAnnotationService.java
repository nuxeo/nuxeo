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
 *
 */

package org.nuxeo.ecm.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOCUMENT_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;

import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.annotation.core")
@Deploy("org.nuxeo.ecm.platform.query.api")
public class TestAnnotationService {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected AnnotationService annotationService;

    protected CloseableCoreSession session;

    @Before
    public void setup() {
        // open a session as a regular user
        session = coreFeature.openCoreSession("jdoe");
        // give permission to him
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE("jdoe", SecurityConstants.READ));
        acl.add(new ACE("jdoe", SecurityConstants.WRITE));
        ACPImpl acp = new ACPImpl();
        acp.addACL(acl);
        coreFeature.getCoreSession().setACP(new PathRef("/"), acp, true);
    }

    @After
    public void tearDown() {
        session.close();
    }

    @Test
    public void testCreateAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String annotationId = "foo";
        String docIdToAnnotate = docToAnnotate.getId();
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = new AnnotationImpl();
        annotation.setId(annotationId);
        annotation.setDocumentId(docIdToAnnotate);
        annotation.setXpath(xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        assertEquals(annotationId, annotation.getId());
        assertEquals(docIdToAnnotate, annotation.getDocumentId());
        assertEquals(xpathToAnnotate, annotation.getXpath());

    }

    @Test
    public void testGetAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String annotationId = "foo";
        String docIdToAnnotate = docToAnnotate.getId();
        String xpathToAnnotate = "files:files/0/file";

        createAnnotationAsDocumentModel(annotationId, docIdToAnnotate, xpathToAnnotate);

        Annotation annotation = annotationService.getAnnotation(session, annotationId);

        assertNotNull("Unable to get the annotation", annotation);
        assertEquals(docIdToAnnotate, annotation.getDocumentId());

    }

    @Test
    public void testUpdateAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String annotationId = "foo";
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = new AnnotationImpl();
        annotation.setId(annotationId);
        annotation.setDocumentId(docToAnnotate.getId());
        annotation.setXpath(xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        assertNull(annotation.getEntity());

        annotation.setEntity("Entity");
        annotationService.updateAnnotation(session, annotation);
        annotation = annotationService.getAnnotation(session, annotation.getId());

        assertEquals("Entity", annotation.getEntity());
    }

    @Test
    public void testDeleteAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String annotationId = "foo";
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = new AnnotationImpl();
        annotation.setId(annotationId);
        annotation.setDocumentId(docToAnnotate.getId());
        annotation.setXpath(xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        annotation = annotationService.getAnnotation(session, annotationId);
        assertNotNull(annotation);

        try {
            annotationService.deleteAnnotation(session, "toto");
            fail("Deleting an unknown annotation should have failed");
        } catch (IllegalArgumentException e) {
            // ok
            assertEquals("The annotation toto does not exist.", e.getMessage());
        }
        annotationService.deleteAnnotation(session, annotation.getId());
        annotation = annotationService.getAnnotation(session, annotationId);
        assertNull(annotation);

    }

    @Test
    public void testGetAnnotationsForDocument() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String xpathToAnnotate = "files:files/0/file";

        List<Annotation> annotations = annotationService.getAnnotations(session, docToAnnotate.getId(), xpathToAnnotate);
        assertTrue(annotations.isEmpty());

        DocumentModel docToAnnotate1 = session.createDocumentModel("/", "testDoc1", "File");
        docToAnnotate1 = session.createDocument(docToAnnotate1);
        int nbAnnotations1 = 99;
        Annotation annotation1 = new AnnotationImpl();
        annotation1.setDocumentId(docToAnnotate1.getId());
        annotation1.setXpath(xpathToAnnotate);
        for (int i = 0; i < nbAnnotations1; i++) {
            annotationService.createAnnotation(session, annotation1);
        }
        DocumentModel docToAnnotate2 = session.createDocumentModel("/", "testDoc2", "File");
        docToAnnotate2 = session.createDocument(docToAnnotate2);
        int nbAnnotations2 = 74;
        Annotation annotation2 = new AnnotationImpl();
        annotation2.setDocumentId(docToAnnotate2.getId());
        annotation2.setXpath(xpathToAnnotate);
        for (int i = 0; i < nbAnnotations2; i++) {
            annotationService.createAnnotation(session, annotation2);
        }
        session.save();
        assertEquals(nbAnnotations1, annotationService.getAnnotations(session, docToAnnotate1.getId(), xpathToAnnotate)
                                                      .size());
        assertEquals(nbAnnotations2, annotationService.getAnnotations(session, docToAnnotate2.getId(), xpathToAnnotate)
                                                      .size());

    }

    protected void createAnnotationAsDocumentModel(String annotationId, String docIdToAnnotate, String xpathToAnnotate) {
        // create an annotation outside of service
        CoreSession adminSession = coreFeature.getCoreSession();
        DocumentModel annotationModel = adminSession.createDocumentModel(null, "testName", ANNOTATION_DOC_TYPE);
        annotationModel.setPropertyValue(ANNOTATION_ID_PROPERTY, annotationId);
        annotationModel.setPropertyValue(ANNOTATION_XPATH_PROPERTY, xpathToAnnotate);
        annotationModel.setPropertyValue(ANNOTATION_DOCUMENT_ID_PROPERTY, docIdToAnnotate);
        adminSession.createDocument(annotationModel);
        adminSession.save();
    }

}
