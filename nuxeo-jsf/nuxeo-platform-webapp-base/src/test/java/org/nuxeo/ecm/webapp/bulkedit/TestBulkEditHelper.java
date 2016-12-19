/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webapp.bulkedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.types.api", "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.webapp.base", "org.nuxeo.ecm.platform.webapp.types" })
public class TestBulkEditHelper {

    @Inject
    protected CoreSession session;

    @Inject
    protected TypeManager typeManager;

    @Test
    public void testCommonSchemas() throws Exception {
        List<DocumentModel> docs = createTestDocuments();
        List<String> commonSchemas = BulkEditHelper.getCommonSchemas(docs);
        assertFalse(commonSchemas.isEmpty());
        assertEquals(5, commonSchemas.size());

        assertTrue(commonSchemas.contains("uid"));
        assertTrue(commonSchemas.contains("dublincore"));
        assertTrue(commonSchemas.contains("common"));
        assertTrue(commonSchemas.contains("files"));
        assertTrue(commonSchemas.contains("relatedtext"));

        assertFalse(commonSchemas.contains("note"));
        assertFalse(commonSchemas.contains("file"));
    }

    protected List<DocumentModel> createTestDocuments() {
        DocumentModel file = session.createDocumentModel("/", "testFile", "File");
        file.setPropertyValue("dc:title", "testTitle");
        file = session.createDocument(file);
        assertNotNull(file);
        file = session.saveDocument(file);

        DocumentModel note = session.createDocumentModel("/", "testFile", "Note");
        note.setPropertyValue("dc:title", "testNote");
        note = session.createDocument(note);
        assertNotNull(note);
        note = session.saveDocument(note);
        session.save();

        return Arrays.asList(file, note);
    }

    @Test
    public void testCommonLayouts() throws Exception {
        List<DocumentModel> docs = createTestDocuments();
        List<String> commonLayouts = BulkEditHelper.getCommonLayouts(typeManager, docs);
        assertFalse(commonLayouts.isEmpty());
        assertEquals(2, commonLayouts.size());
        assertTrue(commonLayouts.contains("heading"));
        assertTrue(commonLayouts.contains("dublincore"));
        assertFalse(commonLayouts.contains("note"));
        assertFalse(commonLayouts.contains("file"));
    }

}
