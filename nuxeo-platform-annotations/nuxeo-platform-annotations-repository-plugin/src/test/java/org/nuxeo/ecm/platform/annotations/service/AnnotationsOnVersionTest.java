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
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.repository.AbstractRepositoryTestCase;
import org.nuxeo.ecm.platform.annotations.repository.FakeNuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.repository.URNDocumentViewTranslator;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class AnnotationsOnVersionTest extends AbstractRepositoryTestCase {

    private static final String SERVER = "http://server.com/nuxeo/";

    private final NuxeoPrincipal user = new FakeNuxeoPrincipal("Administrator");

    private final URNDocumentViewTranslator translator = new URNDocumentViewTranslator();

    public void testAnnotationsOnVersion() throws Exception {
        DocumentModel docModel = createDocument(
                coreSession.getRootDocument().getPathAsString(),
                "fileAnnotationsOnVersion");

        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationOn(url);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(), docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri,
                null, user);
        assertEquals(1, annotations.size());

        VersionModel version = checkIn(docModel.getRef(), "1");

        // still 1 annotation on current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(1, annotations.size());

        // 1 annotation on the version
        DocumentModel versionModel = coreSession.getDocumentWithVersion(
                docModel.getRef(), version);
        URI uriVersion = translator.getNuxeoUrn(versionModel.getRepositoryName(), versionModel.getId());
        annotations = service.queryAnnotations(uriVersion, null, user);
        assertEquals(1, annotations.size());

        addAnnotationOn(url);
        // 2 annotations on the current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(2, annotations.size());

        // but still 1 on the version
        annotations = service.queryAnnotations(uriVersion, null, user);
        assertEquals(1, annotations.size());
    }

    public void testAnnotationsOnRestore() throws Exception {
        DocumentModel docModel = createDocument(
                coreSession.getRootDocument().getPathAsString(),
                "fileAnnotationsOnRestore");
        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationOn(url);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(), docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri,
                null, user);
        assertEquals(1, annotations.size());

        coreSession.checkOut(docModel.getRef());
        VersionModel version = checkIn(docModel.getRef(), "1");

        // still 1 annotation on current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(1, annotations.size());

        // 1 annotation on the version
        DocumentModel versionModel = coreSession.getDocumentWithVersion(
                docModel.getRef(), version);
        URI uriVersion = translator.getNuxeoUrn(versionModel.getRepositoryName(), versionModel.getId());
        annotations = service.queryAnnotations(uriVersion, null, user);
        assertEquals(1, annotations.size());

        addAnnotationOn(url);
        // 2 annotations on the current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(2, annotations.size());

        // but still 1 on the version
        annotations = service.queryAnnotations(uriVersion, null, user);
        assertEquals(1, annotations.size());

        // Restore the first version
        docModel = restoreToVersion(docModel.getRef(), version);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(1, annotations.size());
    }

    public void testAnnotationsOnRestoreWithMultipleVersions() throws Exception {
        DocumentModel docModel = createDocument(
                coreSession.getRootDocument().getPathAsString(),
                "fileAnnotationsOnRestoreWithMultipleVersions");
        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationsOn(url, 3);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(), docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri,
                null, user);
        assertEquals(3, annotations.size());

        VersionModel version1 = checkIn(docModel.getRef(), "1");
        coreSession.checkOut(docModel.getRef());
        List<DocumentModel> versions = coreSession.getVersions(docModel.getRef());
        assertEquals(1, versions.size());

        DocumentModel versionModel1 = coreSession.getDocumentWithVersion(
                docModel.getRef(), version1);
        URI uriVersion1 = translator.getNuxeoUrn(versionModel1.getRepositoryName(), versionModel1.getId());
        annotations = service.queryAnnotations(uriVersion1, null, user);
        assertEquals(3, annotations.size());

        addAnnotationsOn(url, 3);
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(6, annotations.size());

        VersionModel version2 = checkIn(docModel.getRef(), "2");
        coreSession.checkOut(docModel.getRef());
        versions = coreSession.getVersions(docModel.getRef());
        assertEquals(2, versions.size());

        DocumentModel versionModel2 = coreSession.getDocumentWithVersion(
                docModel.getRef(), version2);
        URI uriVersion2 = translator.getNuxeoUrn(versionModel2.getRepositoryName(), versionModel2.getId());

        // 6 annotations on version 2
        annotations = service.queryAnnotations(uriVersion2, null, user);
        assertEquals(6, annotations.size());

        // 6 annotations on current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(6, annotations.size());

        // 3 annotations on version 1
        annotations = service.queryAnnotations(uriVersion1, null, user);
        assertEquals(3, annotations.size());

        // Restore the first version
        docModel = restoreToVersion(docModel.getRef(), version1);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(3, annotations.size());

        // Restore the second version
        docModel = restoreToVersion(docModel.getRef(), version2);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(6, annotations.size());
    }

    public void testDeleteAnnotationsOnVersions() throws Exception {
        DocumentModel docModel = createDocument(
                coreSession.getRootDocument().getPathAsString(),
                "fileDeleteAnnotationsOnVersions");
        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(docModel), true, SERVER);
        assertNotNull(url);
        addAnnotationsOn(url, 3);

        URI uri = translator.getNuxeoUrn(docModel.getRepositoryName(), docModel.getId());

        List<Annotation> annotations = service.queryAnnotations(uri,
                null, user);
        assertEquals(3, annotations.size());

        VersionModel version1 = checkIn(docModel.getRef(), "1");
        coreSession.checkOut(docModel.getRef());
        List<DocumentModel> versions = coreSession.getVersions(docModel.getRef());
        assertEquals(1, versions.size());

        DocumentModel versionModel1 = coreSession.getDocumentWithVersion(
                docModel.getRef(), version1);
        URI uriVersion1 = translator.getNuxeoUrn(versionModel1.getRepositoryName(), versionModel1.getId());
        annotations = service.queryAnnotations(uriVersion1, null, user);
        assertEquals(3, annotations.size());

        addAnnotationsOn(url, 3);
        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(6, annotations.size());

        VersionModel version2 = checkIn(docModel.getRef(), "2");
        coreSession.checkOut(docModel.getRef());
        versions = coreSession.getVersions(docModel.getRef());
        assertEquals(2, versions.size());

        DocumentModel versionModel2 = coreSession.getDocumentWithVersion(
                docModel.getRef(), version2);
        URI uriVersion2 = translator.getNuxeoUrn(versionModel2.getRepositoryName(), versionModel2.getId());
        annotations = service.queryAnnotations(uriVersion2, null, user);
        assertEquals(6, annotations.size());


        // Delete annotations for version 1
        annotations = service.queryAnnotations(uriVersion1, null, user);
        for (Annotation annotation : annotations) {
            service.deleteAnnotationFor(uriVersion1, annotation, user);
        }
        annotations = service.queryAnnotations(uriVersion1, null, user);
        assertEquals(0, annotations.size());

        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(6, annotations.size());

        // Delete annotations for version 2
        annotations = service.queryAnnotations(uriVersion2, null, user);
        for (Annotation annotation : annotations) {
            service.deleteAnnotationFor(uriVersion2, annotation, user);
        }
        annotations = service.queryAnnotations(uriVersion2, null, user);
        assertEquals(0, annotations.size());

        annotations = service.queryAnnotations(uriVersion1, null, user);
        assertEquals(0, annotations.size());

        annotations = service.queryAnnotations(uri, null, user);
        assertEquals(6, annotations.size());

        // restore version 1
        docModel = restoreToVersion(docModel.getRef(), version1);
        // 1 annotation on the current document
        annotations = service.queryAnnotations(uri, null, user);
        // still no annotation on the current document
        assertEquals(0, annotations.size());
    }

    protected DocumentModel createDocument(String parentPath, String id)
            throws Exception {
        DocumentModel docModel = coreSession.createDocumentModel(parentPath,
                id, "File");
        docModel = coreSession.createDocument(docModel);
        coreSession.save();
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

    protected VersionModel checkIn(DocumentRef ref, String versionLabel)
            throws Exception {
        VersionModel version = new VersionModelImpl();
        version.setLabel(versionLabel);
        coreSession.checkIn(ref, version);
        coreSession.save();
        waitForAsyncExec();
        return version;
    }

    protected DocumentModel restoreToVersion(DocumentRef docRef,
            VersionModel version) throws Exception {
        coreSession.save();
        DocumentModel docModel = coreSession.restoreToVersion(docRef, version);
        coreSession.save();
        waitForAsyncExec();
        return docModel;
    }

}
