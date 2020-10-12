/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.core.operations.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.Properties;
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
 * @since 10.10
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
public class TestFileManagerImportWithProperties {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService service;

    @Inject
    public TransactionalFeature txFeature;

    @Test
    public void testNoteCreationWithProperties() throws OperationException {
        Blob textBlob = Blobs.createBlob("foo", "text/plain", null, "foo.txt");
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.put("currentDocument", "/");
            ctx.setInput(textBlob);
            Map<String, Serializable> params = new HashMap<>();
            Properties properties = new Properties();
            properties.put("dc:source", "bar");
            params.put("properties", properties);

            DocumentModel doc = (DocumentModel) service.run(ctx, FileManagerImportWithProperties.ID, params);
            assertNotNull(doc);
            assertFalse(doc.isDirty());
            assertNotNull(doc.getId());
            assertEquals("Note", doc.getType());
            assertEquals("foo.txt", doc.getTitle());
            assertEquals("foo", doc.getPropertyValue("note:note"));
            assertEquals("bar", doc.getPropertyValue("dc:source"));
            assertEquals("0.1", doc.getVersionLabel()); // only one version of the note is done
        }
    }
}
