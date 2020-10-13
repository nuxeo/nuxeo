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
 *     Mickaël Schoentgen <mschoentgen@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
public class TestFileManagerImport {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Inject
    public TransactionalFeature txFeature;

    protected Blob textBlob;

    @Before
    public void before() {
        textBlob = Blobs.createBlob("foo", "application/octet-stream", null, "foo.txt");
    }

    @Test
    public void testFileImport() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(textBlob);
            ctx.put("currentDocument", "/");

            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerImport.ID);
            assertNotNull(doc);
            assertNotNull(doc.getId());
            // noMimeTypeCheck is false by default, thus a Note should have been created
            assertEquals("Note", doc.getType());
            assertEquals("foo.txt", doc.getName());
            assertEquals("/foo.txt", doc.getPathAsString());
            assertEquals("foo.txt", doc.getTitle());
        }
    }

    @Test
    public void testFileImportNoNoMimeTypeCheck() throws OperationException {
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(textBlob);
            ctx.put("currentDocument", "/");

            Map<String, Serializable> params = Map.of("noMimeTypeCheck", true);
            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerImport.ID, params);
            // no mime type check, thus a File should have been created
            assertEquals("File", doc.getType());
        }
    }

    @Test
    public void testFileImportNoOverwrite() throws OperationException {
        DocumentModel file;

        // create a first document
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(textBlob);
            ctx.put("currentDocument", "/");
            file = (DocumentModel) service.run(ctx, FileManagerImport.ID);
        }

        // no overwrite
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(textBlob);
            ctx.put("currentDocument", "/");

            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerImport.ID);
            assertNotEquals(file.getId(), doc.getId());
        }
    }

    @Test
    public void testFileImportOverwrite() throws OperationException {
        DocumentModel file;

        // create a first document
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(textBlob);
            ctx.put("currentDocument", "/");
            file = (DocumentModel) service.run(ctx, FileManagerImport.ID);
        }

        // overwrite
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(textBlob);
            ctx.put("currentDocument", "/");

            Map<String, Serializable> params = Map.of("overwrite", true);
            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerImport.ID, params);
            assertEquals(file.getId(), doc.getId());
        }
    }

    @Test
    public void testFileImportAliasOverwite() throws OperationException {
        DocumentModel file;

        // create a first document
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(textBlob);
            ctx.put("currentDocument", "/");
            file = (DocumentModel) service.run(ctx, FileManagerImport.ID);
        }

        // overwrite using the alias
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(textBlob);
            ctx.put("currentDocument", "/");

            Map<String, Serializable> params = Map.of("overwite", true);
            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerImport.ID, params);
            assertEquals(file.getId(), doc.getId());
        }
    }
}
