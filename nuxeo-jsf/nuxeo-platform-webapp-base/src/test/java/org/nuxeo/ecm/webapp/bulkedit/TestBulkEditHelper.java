/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
@Deploy( { "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.webapp.base" })
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

    protected List<DocumentModel> createTestDocuments() throws ClientException {
        DocumentModel file = session.createDocumentModel("/", "testFile",
                "File");
        file.setPropertyValue("dc:title", "testTitle");
        file = session.createDocument(file);
        assertNotNull(file);
        file = session.saveDocument(file);

        DocumentModel note = session.createDocumentModel("/", "testFile",
                "Note");
        note.setPropertyValue("dc:title", "testNote");
        note = session.createDocument(note);
        assertNotNull(note);
        note = session.saveDocument(note);
        session.save();

        return Arrays.asList(file,
                note);
    }

    @Test
    public void testCommonLayouts() throws Exception {
        List<DocumentModel> docs = createTestDocuments();
        List<String> commonLayouts = BulkEditHelper.getCommonLayouts(
                typeManager, docs);
        assertFalse(commonLayouts.isEmpty());
        assertEquals(2, commonLayouts.size());
        assertTrue(commonLayouts.contains("heading"));
        assertTrue(commonLayouts.contains("dublincore"));
        assertFalse(commonLayouts.contains("note"));
        assertFalse(commonLayouts.contains("file"));
    }

    @Test
    public void testGetPropertiesToCopy() {
        DocumentModel doc = new SimpleDocumentModel("dublincore");
        ScopedMap map = doc.getContextData();
        map.put(BulkEditHelper.BULK_EDIT_PREFIX + "dc:title", true);
        map.put(BulkEditHelper.BULK_EDIT_PREFIX + "dc:description", false);
        map.put(BulkEditHelper.BULK_EDIT_PREFIX + "dc:coverage dc:subjects", true);
        map.put(BulkEditHelper.BULK_EDIT_PREFIX + "dc:creator", true);

        List<String> propertiesToCopy = BulkEditHelper.getPropertiesToCopy(doc);
        assertEquals(4, propertiesToCopy.size());
        assertTrue(propertiesToCopy.contains("dc:title"));
        assertTrue(propertiesToCopy.contains("dc:coverage"));
        assertTrue(propertiesToCopy.contains("dc:subjects"));
        assertTrue(propertiesToCopy.contains("dc:creator"));
        assertFalse(propertiesToCopy.contains("dc:description"));
    }

    @Test
    public void testCopyMetadata() throws Exception {
        List<DocumentModel> docs = createTestDocuments();
        List<String> commonSchemas = BulkEditHelper.getCommonSchemas(docs);
        DocumentModel sourceDoc = new SimpleDocumentModel(commonSchemas);
        sourceDoc.setProperty("dublincore", "title", "new title");
        sourceDoc.setProperty("dublincore", "description", "new description");
        sourceDoc.setProperty("dublincore", "creator", "new creator");
        sourceDoc.setProperty("dublincore", "source", "new source");
        ScopedMap map = sourceDoc.getContextData();
        map.put(BulkEditHelper.BULK_EDIT_PREFIX + "dc:title", true);
        map.put(BulkEditHelper.BULK_EDIT_PREFIX + "dc:description", false);
        map.put(BulkEditHelper.BULK_EDIT_PREFIX + "dc:creator", true);
        map.put(BulkEditHelper.BULK_EDIT_PREFIX + "dc:source", false);

        BulkEditHelper.copyMetadata(session, sourceDoc, docs);
        for (DocumentModel doc : docs) {
            assertEquals("new title", doc.getPropertyValue("dc:title"));
            assertEquals("new creator", doc.getPropertyValue("dc:creator"));
            assertFalse("new description".equals(doc.getPropertyValue("dc:description")));
            assertFalse("new source".equals(doc.getPropertyValue("dc:source")));
        }
    }

}
