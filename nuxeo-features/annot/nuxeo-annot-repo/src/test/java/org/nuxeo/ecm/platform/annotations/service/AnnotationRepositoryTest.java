/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.repository.AbstractRepositoryTestCase;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class AnnotationRepositoryTest extends AbstractRepositoryTestCase {

    private static final Log log = LogFactory.getLog(AnnotationRepositoryTest.class);

    private static final String SERVER1 = "http://server1.com/nuxeo/";

    private static final String SERVER2 = "http://server2.com/nuxeo/";

    private DocumentModel version1;

    private final NuxeoPrincipal user = new UserPrincipal("bob",
            new ArrayList<String>(), false, false);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setUpRepository();
    }

    @Test
    public void testAnnotateDocuments() throws Exception {
        waitForAsyncExec();
        assertNotNull(session);
        DocumentModel myfileModel = session.createDocumentModel(
                session.getRootDocument().getPathAsString(), "999", "File");
        DocumentModel myfile = session.createDocument(myfileModel);
        assertNotNull(myfile);

        session.save();
        closeSession();
        waitForAsyncExec();
        DatabaseHelper.DATABASE.sleepForFulltext();
        openSession();

        // the text 'zombie' is not found in the document
        DocumentModelList results = session.query(
                "SELECT * FROM Document WHERE ecm:fulltext = 'zombie'", 10);
        assertEquals(0, results.size());

        String uriMyfileServer1 = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(myfile), true, SERVER1);
        assertNotNull(uriMyfileServer1);
        String uriMyFileserver2 = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(myfile), true, SERVER2);
        assertNotNull(uriMyFileserver2);
        Annotation annotation = getAnnotation(uriMyfileServer1, 1);
        annotation.setBodyText("This is a Zombie annotation text");
        service.addAnnotation(annotation, user, SERVER1);
        sameDocumentFrom2Servers(uriMyfileServer1, uriMyFileserver2);

        session.save();
        closeSession();
        waitForAsyncExec();
        DatabaseHelper.DATABASE.sleepForFulltext();
        openSession();

        // the body of the text is annotated on the document
        results = session.query(
                "SELECT * FROM Document WHERE ecm:fulltext = 'zombie'", 10);
        assertEquals(1, results.size());
        assertEquals(myfile.getRef(), results.get(0).getRef());

        // create a new document version: the annotation are held by the live
        // document, the archive document do not have annotation by default
        // hence not findable by fulltext search
        myfile = session.getDocument(myfile.getRef());
        myfile.putContextData(VersioningService.VERSIONING_OPTION,
                VersioningOption.MAJOR);
        myfile.putContextData(VersioningService.CHECKIN_COMMENT,
                "I would like to create a new major version");
        myfile = session.saveDocument(myfile);

        session.save();
        closeSession();
        waitForAsyncExec();
        DatabaseHelper.DATABASE.sleepForFulltext();
        openSession();

        // we  find no documents: this is
        // intentional: even if the annotation have been copied to the archived
        // versioned, but not the potential proxies pointed to them, we do not
        // want to fulltext index the body of annotation on achived versions
        results = session.query(
                "SELECT * FROM Document WHERE ecm:fulltext = 'zombie'", 10);
        // TODO fails randomly due to random async event execution order
        // assertEquals(0, results.size());
        //
        // newVersionSameAnnotations(session, myfile, uriMyfileServer1);
        // annotationOnNewVersion(uriMyfileServer1);
    }

    protected void annotationOnNewVersion(String u1)
            throws AnnotationException, IOException, URISyntaxException {
        annotation = service.addAnnotation(getAnnotation(u1, 2), user, SERVER1);
        assertNotNull(annotation);
        List<Annotation> annotations = service.queryAnnotations(new URI(u1),
                null, user);
        assertEquals(1, annotations.size());
        String versionUrl = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(version1), true, SERVER1);
        annotations = service.queryAnnotations(new URI(versionUrl), null, user);
        assertEquals(1, annotations.size());
    }

    protected void newVersionSameAnnotations(CoreSession session,
            DocumentModel myfile, String uriAnnotatedDoc)
            throws AnnotationException, URISyntaxException, ClientException {
        List<Annotation> annotations = service.queryAnnotations(new URI(
                uriAnnotatedDoc), null, user);
        log.debug(annotations.size() + " annotations for: " + uriAnnotatedDoc);
        assertEquals(0, annotations.size());
        List<DocumentModel> versions = session.getVersions(myfile.getRef());
        assertEquals(1, versions.size());
        version1 = versions.get(0);
        String versionUrl = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(version1), true, SERVER1);
        assertNotNull(versionUrl);
        annotations = service.queryAnnotations(new URI(versionUrl), null, user);
        log.debug(annotations.size() + " annotations for: " + versionUrl);
        assertEquals(1, annotations.size());
    }

    protected void createVersion(CoreSession session, DocumentModel myfile,
            String comment) throws ClientException {
        session.checkIn(myfile.getRef(), VersioningOption.MAJOR, comment);
        session.checkOut(myfile.getRef());
        session.save();
        waitForAsyncExec();
    }

    protected void sameDocumentFrom2Servers(String u1, String u2)
            throws AnnotationException, URISyntaxException {
        List<Annotation> annotations = service.queryAnnotations(new URI(u1),
                null, user);
        assertEquals(1, annotations.size());
        annotations = service.queryAnnotations(new URI(u2), null, user);
        assertEquals(1, annotations.size());
    }

}
