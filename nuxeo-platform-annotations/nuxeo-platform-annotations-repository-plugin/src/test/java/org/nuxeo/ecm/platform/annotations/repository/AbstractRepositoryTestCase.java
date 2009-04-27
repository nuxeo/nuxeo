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

package org.nuxeo.ecm.platform.annotations.repository;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.hsqldb.jdbc.jdbcDataSource;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 *
 */
public abstract class AbstractRepositoryTestCase extends RepositoryOSGITestCase {
    protected final AnnotationManager manager = new AnnotationManager();

    protected URI uri;

    protected Annotation annotation;

    protected AnnotationsService service;

    protected DocumentViewCodecManager viewCodecManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:jena");
        ds.setUser("sa");
        ds.setPassword("");
        Context context = new InitialContext();
        context.bind("java:/nxrelations-default-jena", ds);
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseType", "HSQL");
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseTransactionEnabled", "false");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.annotations");
        deployBundle("org.nuxeo.ecm.annotations.repository");
        deployBundle("org.nuxeo.ecm.platform.url.core");
        deployBundle("org.nuxeo.ecm.platform.url.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployBundle("org.nuxeo.ecm.annotations.repository.test");
        service = Framework.getService(AnnotationsService.class);
        viewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        assertNotNull(viewCodecManager);
        assertNotNull(manager);
        InputStream is = getClass().getResourceAsStream(
                "/annotea-spec-post.xml");
        assertNotNull(is);
        annotation = manager.getAnnotation(is);
        openRepository();
    }

    protected void setUpRepository() throws Exception {
        // create structure
        CoreSession session = getCoreSession();
        assertNotNull(session);
        DocumentModel root = session.getRootDocument();
        assertNotNull(root);
        DocumentModel model = session.createDocumentModel(
                root.getPathAsString(), "1", "File");
        DocumentModel sectionModel = session.createDocumentModel(
                root.getPathAsString(), "2", "Section");
        assertNotNull(sectionModel);
        DocumentModel section = session.createDocument(sectionModel);
        assertNotNull(section);
        DocumentModel section1Model = session.createDocumentModel(
                section.getPathAsString(), "3", "Folder");
        DocumentModel section1 = session.createDocument(section1Model);
        assertNotNull(section1);
        DocumentModel section2Model = session.createDocumentModel(
                section.getPathAsString(), "3", "Folder");
        DocumentModel section2 = session.createDocument(section2Model);
        assertNotNull(section2);
        DocumentModel section3Model = session.createDocumentModel(
                section.getPathAsString(), "3", "Folder");
        DocumentModel section3 = session.createDocument(section3Model);
        assertNotNull(section3);
        DocumentModel doc = session.createDocument(model);
        assertNotNull(doc);
        session.saveDocument(doc);
        // create version
        VersionModel versionModel1 = new VersionModelImpl();
        versionModel1.setLabel("v1");
        coreSession.checkIn(doc.getRef(), versionModel1);
        coreSession.checkOut(doc.getRef());
        session.saveDocument(doc);
        session.save();
        VersionModel versionModel2 = new VersionModelImpl();
        versionModel2.setLabel("v2");
        coreSession.checkIn(doc.getRef(), versionModel2);
        coreSession.checkOut(doc.getRef());
        session.saveDocument(doc);
        session.save();
        VersionModel versionModel3 = new VersionModelImpl();
        versionModel3.setLabel("v3");
        coreSession.checkIn(doc.getRef(), versionModel3);
        coreSession.checkOut(doc.getRef());
        session.saveDocument(doc);
        session.save();
        List<DocumentModel> l = session.getVersions(doc.getRef());
        assertFalse(doc.isVersion());
        assertEquals(3, l.size());
        // create proxies
        session.createProxy(section1.getRef(), doc.getRef(), versionModel1,
                true);
        session.createProxy(section2.getRef(), doc.getRef(), versionModel2,
                true);
        session.createProxy(section3.getRef(), doc.getRef(), versionModel3,
                true);
        session.save();
        List<DocumentModel> proxies = session.getProxies(doc.getRef(), null);
        assertNotNull(proxies);
        assertEquals(3, proxies.size());
        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(doc), true, "http://localhost/nuxeo/");
        assertNotNull(url);
        uri = new URI(url.toString());
    }

    protected Annotation getAnnotation(String url, int x) throws IOException,
            AnnotationException {

        InputStream is = getClass().getResourceAsStream(
                "/annotation" + x + ".xml");
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String s;
        StringBuilder builder = new StringBuilder();
        while ((s = br.readLine()) != null) {
            builder.append(s);
        }
        s = builder.toString();
        s = s.replaceAll("docUrl", url);
        is = new ByteArrayInputStream(s.getBytes("UTF-8"));
        return manager.getAnnotation(is);
    }

    protected void waitForAsyncExec() {
        EventServiceImpl evtService = (EventServiceImpl) Framework.getLocalService(EventService.class);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while ((evtService.getActiveAsyncTaskCount()) > 0) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
