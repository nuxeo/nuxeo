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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_CREATION_DATE_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOCUMENT_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_LAST_MODIFIER_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_NAME_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_PAGE_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_POSITION_PROPERTY;

import java.util.Calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 10.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.annotation.core", "org.nuxeo.ecm.platform.query.api" })
public class TestAnnotationService {

    @Inject
    protected CoreSession session;

    @Inject
    protected AnnotationService annotationService;

    @Test
    public void testCreateAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String annotationName = "annotationName";
        String xpathToAnnotate = "files:files/0/file";
        Calendar annotationDate = Calendar.getInstance();
        long annotationPage = 42L;

        Annotation annotation = new AnnotationImpl();
        annotation.setName(annotationName);
        annotation.setCreationDate(annotationDate);
        annotation.setPage(annotationPage);

        annotation = annotationService.createAnnotation(session, docToAnnotate.getId(), xpathToAnnotate, annotation);

        DocumentModel annotationModel = session.getDocument(new IdRef(annotation.getId()));

        assertNotNull(annotationModel);
        assertEquals(docToAnnotate.getId(), annotationModel.getPropertyValue(ANNOTATION_DOCUMENT_ID_PROPERTY));
        assertEquals(xpathToAnnotate, annotationModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY));
        assertEquals(annotationName, annotationModel.getPropertyValue(ANNOTATION_NAME_PROPERTY));
        assertEquals(annotationDate, annotationModel.getPropertyValue(ANNOTATION_CREATION_DATE_PROPERTY));
        assertEquals(annotationPage, annotationModel.getPropertyValue(ANNOTATION_PAGE_PROPERTY));

    }

    @Test
    public void testGetAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        DocumentModel annotationModel = session.createDocumentModel(null, "testName", ANNOTATION_DOC_TYPE);

        String xpathToAnnotate = "files:files/0/file";
        String annotationLastModifier = "bob";
        long annotationPage = 13L;
        String annotationPosition = "0,0,0,0";

        annotationModel.setPropertyValue(ANNOTATION_DOCUMENT_ID_PROPERTY, docToAnnotate.getId());
        annotationModel.setPropertyValue(ANNOTATION_LAST_MODIFIER_PROPERTY, annotationLastModifier);
        annotationModel.setPropertyValue(ANNOTATION_PAGE_PROPERTY, annotationPage);
        annotationModel.setPropertyValue(ANNOTATION_POSITION_PROPERTY, annotationPosition);

        annotationModel = session.createDocument(annotationModel);
        session.save();

        Annotation annotation = annotationService.getAnnotation(session, docToAnnotate.getId(), xpathToAnnotate,
                annotationModel.getId());

        assertEquals(docToAnnotate.getId(), annotation.getDocumentId());
        assertEquals(annotationLastModifier, annotation.getLastModifier());
        assertEquals(annotationPage, annotation.getPage());
        assertEquals(annotationPosition, annotation.getPosition());
        assertEquals(0d, annotation.getOpacity(), 0d);
        assertNull(annotation.getSecurity());

    }

    @Test
    public void testUpdateAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String xpathToAnnotate = "files:files/0/file";
        String annotationPosition = "0,0,0,0";
        long annotationPage = 42L;

        Annotation annotation = new AnnotationImpl();
        annotation.setPosition(annotationPosition);
        annotation.setPage(annotationPage);

        annotation = annotationService.createAnnotation(session, docToAnnotate.getId(), xpathToAnnotate, annotation);

        assertEquals(annotationPage, annotation.getPage());
        assertEquals(annotationPosition, annotation.getPosition());
        assertNull(annotation.getSubject());

        long newAnnotationPage = 35L;
        String annotationSubject = "testSubject";

        annotation.setPage(newAnnotationPage);
        annotation.setSubject(annotationSubject);

        annotationService.updateAnnotation(session, docToAnnotate.getId(), xpathToAnnotate, annotation);
        annotation = annotationService.getAnnotation(session, docToAnnotate.getId(), xpathToAnnotate,
                annotation.getId());

        assertEquals(newAnnotationPage, annotation.getPage());
        assertEquals(annotationPosition, annotation.getPosition());
        assertEquals(annotationSubject, annotation.getSubject());

    }

    @Test
    public void testDeleteAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        Annotation annotation = new AnnotationImpl();
        String xpathToAnnotate = "files:files/0/file";

        annotation = annotationService.createAnnotation(session, docToAnnotate.getId(), xpathToAnnotate, annotation);
        assertTrue(session.exists(new IdRef(annotation.getId())));

        try {
            annotationService.deleteAnnotation(session, docToAnnotate.getId(), xpathToAnnotate, "toto");
            fail("Deleting an unknown annotation should have failed");
        } catch (IllegalArgumentException e) {
            // ok
            annotationService.deleteAnnotation(session, docToAnnotate.getId(), xpathToAnnotate, annotation.getId());
            assertFalse(session.exists(new IdRef(annotation.getId())));
        }

    }

    @Test
    public void testGetAnnotationsForDocument() {

        DocumentModel docToAnnotate = session.createDocumentModel("/", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String xpathToAnnotate = "files:files/0/file";

        assertEquals(0, annotationService.getAnnotations(session, docToAnnotate.getId(), xpathToAnnotate).size());

        DocumentModel docToAnnotate1 = session.createDocumentModel("/", "testDoc1", "File");
        docToAnnotate1 = session.createDocument(docToAnnotate1);
        int nbAnnotations1 = 99;
        for (int i = 0; i < nbAnnotations1; i++) {
            annotationService.createAnnotation(session, docToAnnotate1.getId(), xpathToAnnotate, new AnnotationImpl());
        }
        DocumentModel docToAnnotate2 = session.createDocumentModel("/", "testDoc2", "File");
        docToAnnotate2 = session.createDocument(docToAnnotate2);
        int nbAnnotations2 = 74;
        for (int i = 0; i < nbAnnotations2; i++) {
            annotationService.createAnnotation(session, docToAnnotate2.getId(), xpathToAnnotate, new AnnotationImpl());
        }
        session.save();
        assertEquals(nbAnnotations1,
                annotationService.getAnnotations(session, docToAnnotate1.getId(), xpathToAnnotate).size());
        assertEquals(nbAnnotations2,
                annotationService.getAnnotations(session, docToAnnotate2.getId(), xpathToAnnotate).size());

    }

}
