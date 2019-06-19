/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.repository.AbstractRepositoryTestCase;
import org.nuxeo.ecm.platform.annotations.repository.URNDocumentViewTranslator;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class AnnotationsOnVersionTest extends AbstractRepositoryTestCase {

    private static final Log log = LogFactory.getLog(AnnotationsOnVersionTest.class);

    private static final String SERVER = "http://server.com/nuxeo/";

    private final NuxeoPrincipal user = new UserPrincipal(SecurityConstants.ADMINISTRATOR, new ArrayList<String>(),
            false, false);

    private final URNDocumentViewTranslator translator = new URNDocumentViewTranslator();

    @Test
    public void testAnnotationsOnVersion() throws Exception {
        DocumentModel docModel = createDocument(session.getRootDocument().getPathAsString(), "fileAnnotationsOnVersion");

        String url = viewCodecManager.getUrlFromDocumentView(new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationOn(url);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(), docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri, user);
        assertEquals(1, annotations.size());

        DocumentRef versionRef = checkIn(docModel.getRef());

        // annotation reset on current document
        annotations = service.queryAnnotations(uri, user);
        assertEquals(0, annotations.size());
        log.debug(annotations.size() + " annotations for: " + uri);
        // 1 annotation on the version
        DocumentModel versionModel = session.getDocument(versionRef);
        URI uriVersion = translator.getNuxeoUrn(versionModel.getRepositoryName(), versionModel.getId());
        annotations = service.queryAnnotations(uriVersion, user);
        log.debug(annotations.size() + " annotations for: " + uriVersion);
        assertEquals(1, annotations.size());

        addAnnotationOn(url);
        // 2 annotations on the current document
        annotations = service.queryAnnotations(uri, user);
        log.debug(annotations.size() + " annotations for: " + uri);
        assertEquals(1, annotations.size());

        // but still 1 on the version
        annotations = service.queryAnnotations(uriVersion, user);
        log.debug(annotations.size() + " annotations for: " + uriVersion);
        assertEquals(1, annotations.size());

    }

    @Test
    public void testAnnotationsOnRestore() throws Exception {
        DocumentModel docModel = createDocument(session.getRootDocument().getPathAsString(), "fileAnnotationsOnRestore");
        String url = viewCodecManager.getUrlFromDocumentView(new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationOn(url);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(), docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri, user);
        assertEquals(1, annotations.size());

        DocumentRef versionRef = checkIn(docModel.getRef());

        // annotation reset on current document
        annotations = service.queryAnnotations(uri, user);
        assertEquals(0, annotations.size());

        // 1 annotation on the version
        DocumentModel versionModel = session.getDocument(versionRef);
        URI uriVersion = translator.getNuxeoUrn(versionModel.getRepositoryName(), versionModel.getId());
        annotations = service.queryAnnotations(uriVersion, user);
        assertEquals(1, annotations.size());

        addAnnotationOn(url);
        // 1 new annotation on document
        annotations = service.queryAnnotations(uri, user);
        assertEquals(1, annotations.size());

        // and still 1 on the version
        annotations = service.queryAnnotations(uriVersion, user);
        assertEquals(1, annotations.size());

        // Restore the first version
        docModel = restoreToVersion(docModel.getRef(), versionRef);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, user);
        assertEquals(1, annotations.size());
    }

    @Test
    public void testAnnotationsOnRestoreWithMultipleVersions() throws Exception {
        DocumentModel docModel = createDocument(session.getRootDocument().getPathAsString(),
                "fileAnnotationsOnRestoreWithMultipleVersions");
        String url = viewCodecManager.getUrlFromDocumentView(new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationsOn(url, 3);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(), docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri, user);
        assertEquals(3, annotations.size());

        DocumentRef version1ref = checkIn(docModel.getRef());
        List<DocumentModel> versions = session.getVersions(docModel.getRef());
        assertEquals(1, versions.size());

        DocumentModel versionModel1 = session.getDocument(version1ref);
        URI uriVersion1 = translator.getNuxeoUrn(versionModel1.getRepositoryName(), versionModel1.getId());
        annotations = service.queryAnnotations(uriVersion1, user);
        assertEquals(3, annotations.size());

        addAnnotationsOn(url, 3);
        annotations = service.queryAnnotations(uri, user);
        assertEquals(3, annotations.size());

        DocumentRef version2ref = checkIn(docModel.getRef());
        versions = session.getVersions(docModel.getRef());
        assertEquals(2, versions.size());

        DocumentModel versionModel2 = session.getDocument(version2ref);
        URI uriVersion2 = translator.getNuxeoUrn(versionModel2.getRepositoryName(), versionModel2.getId());

        // 3 annotations on version 2
        annotations = service.queryAnnotations(uriVersion2, user);
        assertEquals(3, annotations.size());

        // annotations reset on current document
        annotations = service.queryAnnotations(uri, user);
        assertEquals(0, annotations.size());

        // 3 annotations on version 1
        annotations = service.queryAnnotations(uriVersion1, user);
        assertEquals(3, annotations.size());

        // Restore the first version
        docModel = restoreToVersion(docModel.getRef(), version1ref);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, user);
        assertEquals(3, annotations.size());

        // Restore the second version
        docModel = restoreToVersion(docModel.getRef(), version2ref);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, user);
        assertEquals(3, annotations.size());
    }

    @Test
    public void testDeleteAnnotationsOnVersions() throws Exception {
        DocumentModel docModel = createDocument(session.getRootDocument().getPathAsString(),
                "fileDeleteAnnotationsOnVersions");
        String url = viewCodecManager.getUrlFromDocumentView(new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationsOn(url, 3);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(), docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri, user);
        assertEquals(3, annotations.size());

        DocumentRef version1ref = checkIn(docModel.getRef());
        List<DocumentModel> versions = session.getVersions(docModel.getRef());
        assertEquals(1, versions.size());

        DocumentModel versionModel1 = session.getDocument(version1ref);
        URI uriVersion1 = translator.getNuxeoUrn(versionModel1.getRepositoryName(), versionModel1.getId());
        annotations = service.queryAnnotations(uriVersion1, user);
        assertEquals(3, annotations.size());

        addAnnotationsOn(url, 3);
        annotations = service.queryAnnotations(uri, user);
        assertEquals(3, annotations.size());

        DocumentRef version2ref = checkIn(docModel.getRef());
        versions = session.getVersions(docModel.getRef());
        assertEquals(2, versions.size());

        DocumentModel versionModel2 = session.getDocument(version2ref);
        URI uriVersion2 = translator.getNuxeoUrn(versionModel2.getRepositoryName(), versionModel2.getId());
        annotations = service.queryAnnotations(uriVersion2, user);
        assertEquals(3, annotations.size());

        // the text 'zombie' is not found in the document
        DocumentModelList results = session.query("SELECT * FROM Document WHERE ecm:fulltext = 'zombie'", 10);
        assertEquals(0, results.size());

        // put an text annotation a checked in version to check the fulltext
        // indexing
        Annotation annotationVersion1 = getAnnotation(uriVersion1.toASCIIString(), 1);
        annotationVersion1.setBodyText("zombie annotation");
        service.addAnnotation(annotationVersion1, user, uriVersion1.toASCIIString());

        session.save();
        nextTransaction();
        sleepForFulltext();

        results = session.query("SELECT * FROM Document WHERE ecm:fulltext = 'zombie'", 10);
        assertEquals(1, results.size());
        assertEquals(version1ref, results.get(0).getRef());

        // Delete annotations for version 1
        annotations = service.queryAnnotations(uriVersion1, user);
        for (Annotation annotation : annotations) {
            service.deleteAnnotationFor(uriVersion1, annotation, user);
        }
        annotations = service.queryAnnotations(uriVersion1, user);
        assertEquals(0, annotations.size());

        // Delete annotations for version 2
        annotations = service.queryAnnotations(uriVersion2, user);
        for (Annotation annotation : annotations) {
            service.deleteAnnotationFor(uriVersion2, annotation, user);
        }
        annotations = service.queryAnnotations(uriVersion2, user);
        assertEquals(0, annotations.size());

        // restore version 1
        docModel = restoreToVersion(docModel.getRef(), version1ref);
        annotations = service.queryAnnotations(uri, user);
        // still no annotation on the current document
        assertEquals(0, annotations.size());
    }

    protected DocumentModel createDocument(String parentPath, String id) throws Exception {
        DocumentModel docModel = session.createDocumentModel(parentPath, id, "File");
        docModel = session.createDocument(docModel);
        session.save();
        return docModel;
    }

    protected Annotation addAnnotationOn(String url) throws Exception {
        Annotation annotation = getAnnotation(url, 1);
        return service.addAnnotation(annotation, user, SERVER);
    }

    protected void addAnnotationsOn(String url, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            addAnnotationOn(url);
        }
    }

    protected DocumentRef checkIn(DocumentRef ref) throws Exception {
        session.save();
        DocumentRef v = session.checkIn(ref, null, null);
        session.checkOut(ref);
        session.save();
        nextTransaction();
        return v;
    }

    protected DocumentModel restoreToVersion(DocumentRef docRef, DocumentRef versionRef) throws Exception {
        session.save();
        DocumentModel docModel = session.restoreToVersion(docRef, versionRef);
        session.save();
        nextTransaction();
        return docModel;
    }

}
