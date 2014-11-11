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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

import javax.inject.Inject;

import org.h2.util.IOUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotationFeature;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Alexandre Russel
 *
 */
@RunWith(FeaturesRunner.class)
@Features(AnnotationFeature.class)
public abstract class AbstractRepositoryTestCase {
    protected final AnnotationManager manager = new AnnotationManager();

    protected URI uri;

    protected Annotation annotation;

    @Inject
    protected AnnotationsService service;

    @Inject
    protected DocumentViewCodecManager viewCodecManager;

    @Inject
    protected CoreSession session;

    @Inject
    RepositorySettings repo;

    @Before
    public void setUp() throws Exception {
        assertNotNull(viewCodecManager);
        assertNotNull(manager);
        InputStream is = getClass().getResourceAsStream(
                "/annotea-spec-post.xml");
        assertNotNull(is);
        annotation = manager.getAnnotation(is);
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
        EventServiceImpl evtService = (EventServiceImpl) Framework
            .getLocalService(EventService.class);
        evtService.waitForAsyncCompletion();
    }

    protected void openSession() {
        session = repo.createSession();
    }

    protected void closeSession() {
        repo.releaseSession();
        session = null;
    }
}
