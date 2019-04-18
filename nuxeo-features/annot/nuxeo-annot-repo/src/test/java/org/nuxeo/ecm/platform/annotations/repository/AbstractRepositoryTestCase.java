/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
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
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotationFeature;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author Alexandre Russel
 */
@RunWith(FeaturesRunner.class)
@Features(AnnotationFeature.class)
public abstract class AbstractRepositoryTestCase {

    protected final AnnotationManager manager = new AnnotationManager();

    protected URI uri;

    protected Annotation annotation;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected AnnotationsService service;

    @Inject
    protected DocumentViewCodecManager viewCodecManager;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Before
    public void setUp() throws Exception {
        assertNotNull(viewCodecManager);
        assertNotNull(manager);
        try (InputStream is = getClass().getResourceAsStream("/annotea-spec-post.xml")) {
            assertNotNull(is);
            annotation = manager.getAnnotation(is);
        }
    }

    protected void setUpRepository() throws Exception {
        uri = setUpRepository(session);
    }

    /**
     * Set up repository with the input core session and return the URI of created document.
     *
     * @param session the session to initialize
     * @return the URI of created document
     */
    protected URI setUpRepository(CoreSession session) throws Exception {
        PathRef rootRef = new PathRef("/");
        String file1Name = "1";
        DocumentModel doc;
        if (session.hasChild(rootRef, file1Name)) {
            doc = session.getChild(rootRef, file1Name);
        } else {
            // create structure
            assertNotNull(session);
            DocumentModel root = session.getRootDocument();
            assertNotNull(root);
            DocumentModel model = session.createDocumentModel(root.getPathAsString(), file1Name, "File");
            DocumentModel sectionModel = session.createDocumentModel(root.getPathAsString(), "2", "Section");
            assertNotNull(sectionModel);
            DocumentModel section = session.createDocument(sectionModel);
            assertNotNull(section);
            DocumentModel section1Model = session.createDocumentModel(section.getPathAsString(), "3a", "Folder");
            DocumentModel section1 = session.createDocument(section1Model);
            assertNotNull(section1);
            DocumentModel section2Model = session.createDocumentModel(section.getPathAsString(), "3b", "Folder");
            DocumentModel section2 = session.createDocument(section2Model);
            assertNotNull(section2);
            DocumentModel section3Model = session.createDocumentModel(section.getPathAsString(), "3c", "Folder");
            DocumentModel section3 = session.createDocument(section3Model);
            assertNotNull(section3);
            doc = session.createDocument(model);
            assertNotNull(doc);
            doc.setPropertyValue("dc:description", null); // dirty it
            session.saveDocument(doc);
            // create proxies
            session.publishDocument(doc, section1);
            doc.setPropertyValue("dc:description", ""); // dirty it
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
        }
        String url = viewCodecManager.getUrlFromDocumentView(new DocumentViewImpl(doc), true, "http://localhost/nuxeo/");
        assertNotNull(url);
        URI uri = new URI(url);
        nextTransaction();
        return uri;
    }

    protected Annotation getAnnotation(String url, int x) throws IOException {

        String template;
        try (InputStream is = getClass().getResourceAsStream("/annotation" + x + ".xml")) {
            template = IOUtils.readStringAndClose(new InputStreamReader(is), -1);
        }
        template = template.replaceAll("docUrl", url);
        try (InputStream is = new ByteArrayInputStream(template.getBytes("UTF-8"))) {
            return manager.getAnnotation(is);
        }
    }

    protected void sleepForFulltext() {
        coreFeature.getStorageConfiguration().sleepForFulltext();
    }

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

}
