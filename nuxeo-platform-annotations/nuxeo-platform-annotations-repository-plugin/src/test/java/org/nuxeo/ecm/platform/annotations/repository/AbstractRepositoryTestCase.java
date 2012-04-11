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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;

import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import org.h2.util.IOUtils;
import org.hsqldb.jdbc.jdbcDataSource;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * @author Alexandre Russel
 *
 */
public abstract class AbstractRepositoryTestCase extends SQLRepositoryTestCase {
    protected final AnnotationManager manager = new AnnotationManager();

    protected URI uri;

    protected Annotation annotation;

    protected AnnotationsService service;

    protected DocumentViewCodecManager viewCodecManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        NuxeoContainer.installNaming();

        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:jena");
        ds.setUser("sa");
        ds.setPassword("");
        Context context = new InitialContext();
        try {
            context.createSubcontext("java:comp");
        } catch (NameAlreadyBoundException e) {
            // already bound when using Jetty NamingContext
        }
        try {
            context.createSubcontext("java:comp/env");
        } catch (NameAlreadyBoundException e) {
            // already bound when using Nuxeo Common NamingContext
        }
        try {
            context.createSubcontext("java:comp/env/jdbc");
        } catch (NameAlreadyBoundException e) {
            // already bound when using Nuxeo Common NamingContext
        }
        context.bind("java:comp/env/jdbc/nxrelations-default-jena", ds);
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseType", "HSQL");
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseTransactionEnabled", "false");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.annotations.contrib");
        deployBundle("org.nuxeo.ecm.annotations");
        deployBundle("org.nuxeo.ecm.annotations.repository");
        deployBundle("org.nuxeo.ecm.annotations.repository.test");
        deployBundle("org.nuxeo.ecm.platform.url.core");
        deployBundle("org.nuxeo.ecm.platform.url.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.relations.jena");

        service = Framework.getService(AnnotationsService.class);
        viewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        assertNotNull(viewCodecManager);
        assertNotNull(manager);
        InputStream is = getClass().getResourceAsStream(
                "/annotea-spec-post.xml");
        assertNotNull(is);
        annotation = manager.getAnnotation(is);
        openSession();
    }

    @After
    public void tearDown() throws Exception {
        try {
            closeSession();
        } finally {
            if (NuxeoContainer.isInstalled()) {
                NuxeoContainer.uninstall();
            }
            super.tearDown();
        }
    }

    protected void setUpRepository() throws Exception {
        // create structure
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
        // create proxies
        session.publishDocument(doc, section1);
        doc.setPropertyValue("dc:description", null); // dirty it
        session.saveDocument(doc);
        session.publishDocument(doc, section2);
        doc.setPropertyValue("dc:description", null); // dirty it
        session.saveDocument(doc);
        session.publishDocument(doc, section3);
        session.save();
        List<DocumentModel> l = session.getVersions(doc.getRef());
        assertFalse(doc.isVersion());
        assertEquals(3, l.size());
        List<DocumentModel> proxies = session.getProxies(doc.getRef(), null);
        assertNotNull(proxies);
        assertEquals(3, proxies.size());
        String url = viewCodecManager.getUrlFromDocumentView(
                new DocumentViewImpl(doc), true, "http://localhost/nuxeo/");
        assertNotNull(url);
        uri = new URI(url.toString());
        waitForAsyncExec();
    }

    protected Annotation getAnnotation(String url, int x) throws IOException,
            AnnotationException {

        InputStream is = getClass().getResourceAsStream(
                "/annotation" + x + ".xml");
        String template = IOUtils.readStringAndClose(new InputStreamReader(is),
                -1);
        template = template.replaceAll("docUrl", url);
        is = new ByteArrayInputStream(template.getBytes("UTF-8"));
        return manager.getAnnotation(is);
    }

    protected void waitForAsyncExec() {
        EventServiceImpl evtService = (EventServiceImpl) Framework.getLocalService(EventService.class);
        evtService.waitForAsyncCompletion();
    }

}
