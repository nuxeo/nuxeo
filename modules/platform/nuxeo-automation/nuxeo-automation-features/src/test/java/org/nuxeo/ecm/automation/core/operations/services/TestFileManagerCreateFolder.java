/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.platform.webapp.types")
public class TestFileManagerCreateFolder {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Inject
    public TransactionalFeature txFeature;

    protected DocumentModel parent;

    @Before
    public void before() {
        parent = session.createDocumentModel("/", "foo", "Folder");
        parent = session.createDocument(parent);
    }

    @Test
    public void testFolderCreation() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(parent);
            Map<String, Serializable> params = new HashMap<>();
            params.put("title", "bar");

            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerCreateFolder.ID, params);
            assertNotNull(doc);
            assertNotNull(doc.getId());
            assertEquals("Folder", doc.getType());
            assertEquals("bar", doc.getName());
            assertEquals("/foo/bar", doc.getPathAsString());
            assertEquals("bar", doc.getTitle());
        }
    }

    @Test
    public void testFolderCreationWithProperties() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(parent);
            Map<String, Serializable> params = new HashMap<>();
            params.put("title", "bar");
            Properties properties = new Properties();
            properties.put("dc:title", "foobar"); // override the "title" param, which is still used for the doc name
            properties.put("dc:description", "foobar");
            params.put("properties", properties);

            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerCreateFolder.ID, params);
            assertNotNull(doc);
            assertNotNull(doc.getId());
            assertEquals("Folder", doc.getType());
            assertEquals("bar", doc.getName());
            assertEquals("/foo/bar", doc.getPathAsString());
            assertEquals("foobar", doc.getTitle());
            assertEquals("foobar", doc.getPropertyValue("dc:description"));
        }
    }

    @Test
    public void testFolderCreationOverwrite() throws OperationException {
        // first create a folder
        DocumentModel folder = session.createDocumentModel(parent.getPathAsString(), "bar", "Folder");
        folder.setPropertyValue("dc:title", "bar");
        folder.setPropertyValue("dc:description", "foobar");
        folder = session.createDocument(folder);
        txFeature.nextTransaction();

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(parent);
            Map<String, Serializable> params = new HashMap<>();
            params.put("title", "bar");
            params.put("overwrite", true);

            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerCreateFolder.ID, params);
            assertNotNull(doc);
            assertNotNull(doc.getId());
            assertEquals(folder.getId(), doc.getId());
            assertEquals("Folder", doc.getType());
            assertEquals("bar", doc.getName());
            assertEquals("/foo/bar", doc.getPathAsString());
            assertEquals("bar", doc.getTitle());
            assertEquals("foobar", doc.getPropertyValue("dc:description"));
        }
    }

    @Test
    public void testFolderCreationNoOverwrite() throws OperationException {
        // first create a folder
        DocumentModel folder = session.createDocumentModel(parent.getPathAsString(), "bar", "Folder");
        folder.setPropertyValue("dc:title", "bar");
        folder.setPropertyValue("dc:description", "foobar");
        folder = session.createDocument(folder);

        // no overwrite
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(parent);
            Map<String, Serializable> params = new HashMap<>();
            params.put("title", "bar");

            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerCreateFolder.ID, params);
            assertNotNull(doc);
            assertNotNull(doc.getId());
            assertNotEquals(folder.getId(), doc.getId());
            assertEquals("Folder", doc.getType());
            assertNotEquals("bar", doc.getName());
            assertNotEquals("/foo/bar", doc.getPathAsString());
            assertEquals("bar", doc.getTitle());
        }
    }
}
