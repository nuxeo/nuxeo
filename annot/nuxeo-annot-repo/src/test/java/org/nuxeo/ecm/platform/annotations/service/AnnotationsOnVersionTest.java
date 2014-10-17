/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
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

    private final NuxeoPrincipal user = new UserPrincipal(
            SecurityConstants.ADMINISTRATOR, new ArrayList<String>(), false,
            false);

    private final URNDocumentViewTranslator translator = new URNDocumentViewTranslator();

    @Test
    public void testAnnotationsOnVersion() throws Exception {
        DocumentModel docModel = createDocument(
                session.getRootDocument().getPathAsString(),
                "fileAnnotationsOnVersion");

        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationOn(url);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(),
                docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri, null, user);
        assertEquals(1, annotations.size());

        DocumentRef versionRef = checkIn(docModel.getRef());

        // annotation reset on current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(0, annotations.size());
        log.debug(annotations.size() + " annotations for: " + uri);
        // 1 annotation on the version
        DocumentModel versionModel = session.getDocument(versionRef);
        URI uriVersion = translator.getNuxeoUrn(
                versionModel.getRepositoryName(), versionModel.getId());
        annotations = service.queryAnnotations(uriVersion, null, user);
        log.debug(annotations.size() + " annotations for: " + uriVersion);
        assertEquals(1, annotations.size());

        addAnnotationOn(url);
        // 2 annotations on the current document
        annotations = service.queryAnnotations(uri, null, user);
        log.debug(annotations.size() + " annotations for: " + uri);
        assertEquals(1, annotations.size());

        // but still 1 on the version
        annotations = service.queryAnnotations(uriVersion, null, user);
        log.debug(annotations.size() + " annotations for: " + uriVersion);
        assertEquals(1, annotations.size());

    }

    @Test
    public void testAnnotationsOnRestore() throws Exception {
        DocumentModel docModel = createDocument(
                session.getRootDocument().getPathAsString(),
                "fileAnnotationsOnRestore");
        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationOn(url);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(),
                docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri, null, user);
        assertEquals(1, annotations.size());

        DocumentRef versionRef = checkIn(docModel.getRef());

        // annotation reset on current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(0, annotations.size());

        // 1 annotation on the version
        DocumentModel versionModel = session.getDocument(versionRef);
        URI uriVersion = translator.getNuxeoUrn(
                versionModel.getRepositoryName(), versionModel.getId());
        annotations = service.queryAnnotations(uriVersion, null, user);
        assertEquals(1, annotations.size());

        addAnnotationOn(url);
        // 1 new annotation on document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(1, annotations.size());

        // and still 1 on the version
        annotations = service.queryAnnotations(uriVersion, null, user);
        assertEquals(1, annotations.size());

        // Restore the first version
        docModel = restoreToVersion(docModel.getRef(), versionRef);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(1, annotations.size());
    }

    @Test
    public void testAnnotationsOnRestoreWithMultipleVersions() throws Exception {
        DocumentModel docModel = createDocument(
                session.getRootDocument().getPathAsString(),
                "fileAnnotationsOnRestoreWithMultipleVersions");
        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationsOn(url, 3);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(),
                docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri, null, user);
        assertEquals(3, annotations.size());

        DocumentRef version1ref = checkIn(docModel.getRef());
        List<DocumentModel> versions = session.getVersions(docModel.getRef());
        assertEquals(1, versions.size());

        DocumentModel versionModel1 = session.getDocument(version1ref);
        URI uriVersion1 = translator.getNuxeoUrn(
                versionModel1.getRepositoryName(), versionModel1.getId());
        annotations = service.queryAnnotations(uriVersion1, null, user);
        assertEquals(3, annotations.size());

        addAnnotationsOn(url, 3);
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(3, annotations.size());

        DocumentRef version2ref = checkIn(docModel.getRef());
        versions = session.getVersions(docModel.getRef());
        assertEquals(2, versions.size());

        DocumentModel versionModel2 = session.getDocument(version2ref);
        URI uriVersion2 = translator.getNuxeoUrn(
                versionModel2.getRepositoryName(), versionModel2.getId());

        // 3 annotations on version 2
        annotations = service.queryAnnotations(uriVersion2, null, user);
        assertEquals(3, annotations.size());

        // annotations reset on current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(0, annotations.size());

        // 3 annotations on version 1
        annotations = service.queryAnnotations(uriVersion1, null, user);
        assertEquals(3, annotations.size());

        // Restore the first version
        docModel = restoreToVersion(docModel.getRef(), version1ref);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(3, annotations.size());

        // Restore the second version
        docModel = restoreToVersion(docModel.getRef(), version2ref);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(3, annotations.size());
    }

    @Test
    public void testDeleteAnnotationsOnVersions() throws Exception {
        DocumentModel docModel = createDocument(
                session.getRootDocument().getPathAsString(),
                "fileDeleteAnnotationsOnVersions");
        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationsOn(url, 3);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(),
                docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri, null, user);
        assertEquals(3, annotations.size());

        DocumentRef version1ref = checkIn(docModel.getRef());
        List<DocumentModel> versions = session.getVersions(docModel.getRef());
        assertEquals(1, versions.size());

        DocumentModel versionModel1 = session.getDocument(version1ref);
        URI uriVersion1 = translator.getNuxeoUrn(
                versionModel1.getRepositoryName(), versionModel1.getId());
        annotations = service.queryAnnotations(uriVersion1, null, user);
        assertEquals(3, annotations.size());

        addAnnotationsOn(url, 3);
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(3, annotations.size());

        DocumentRef version2ref = checkIn(docModel.getRef());
        versions = session.getVersions(docModel.getRef());
        assertEquals(2, versions.size());

        DocumentModel versionModel2 = session.getDocument(version2ref);
        URI uriVersion2 = translator.getNuxeoUrn(
                versionModel2.getRepositoryName(), versionModel2.getId());
        annotations = service.queryAnnotations(uriVersion2, null, user);
        assertEquals(3, annotations.size());

        // the text 'zombie' is not found in the document
        DocumentModelList results = session.query(
                "SELECT * FROM Document WHERE ecm:fulltext = 'zombie'", 10);
        assertEquals(0, results.size());

        // put an text annotation a checked in version to check the fulltext
        // indexing
        Annotation annotationVersion1 = getAnnotation(
                uriVersion1.toASCIIString(), 1);
        annotationVersion1.setBodyText("zombie annotation");
        service.addAnnotation(annotationVersion1, user,
                uriVersion1.toASCIIString());

        session.save();
        closeSession();
        waitForAsyncExec();
        DatabaseHelper.DATABASE.sleepForFulltext();
        openSession();

        results = session.query(
                "SELECT * FROM Document WHERE ecm:fulltext = 'zombie'", 10);
        assertEquals(1, results.size());
        assertEquals(version1ref, results.get(0).getRef());

        // Delete annotations for version 1
        annotations = service.queryAnnotations(uriVersion1, null, user);
        for (Annotation annotation : annotations) {
            service.deleteAnnotationFor(uriVersion1, annotation, user);
        }
        annotations = service.queryAnnotations(uriVersion1, null, user);
        assertEquals(0, annotations.size());

        // Delete annotations for version 2
        annotations = service.queryAnnotations(uriVersion2, null, user);
        for (Annotation annotation : annotations) {
            service.deleteAnnotationFor(uriVersion2, annotation, user);
        }
        annotations = service.queryAnnotations(uriVersion2, null, user);
        assertEquals(0, annotations.size());

        // restore version 1
        docModel = restoreToVersion(docModel.getRef(), version1ref);
        annotations = service.queryAnnotations(uri, null, user);
        // still no annotation on the current document
        assertEquals(0, annotations.size());
    }

    protected DocumentModel createDocument(String parentPath, String id)
            throws Exception {
        DocumentModel docModel = session.createDocumentModel(parentPath, id,
                "File");
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
        waitForAsyncExec();
        return v;
    }

    protected DocumentModel restoreToVersion(DocumentRef docRef,
            DocumentRef versionRef) throws Exception {
        session.save();
        DocumentModel docModel = session.restoreToVersion(docRef, versionRef);
        session.save();
        waitForAsyncExec();
        return docModel;
    }

}
