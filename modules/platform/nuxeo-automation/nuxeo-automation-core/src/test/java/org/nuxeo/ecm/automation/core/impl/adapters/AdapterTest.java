/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benjamin JALON
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.AutomationCoreFeature;
import org.nuxeo.ecm.automation.core.impl.adapters.helper.AbsoluteDocumentRef;
import org.nuxeo.ecm.automation.core.impl.adapters.helper.TypeAdapterHelper;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationCoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core:test-other-repository-contrib.xml")
public class AdapterTest {

    @Inject
    protected AutomationService automationService;

    @Inject
    protected CoreSession session;

    protected CoreSession sessionOther;

    protected DocumentModel documentModel;

    protected DocumentModel docOther;

    protected OperationContext ctx;

    @Before
    public void setup() {
        ctx = new OperationContext(session);
    }

    @Before
    public void initRepo() throws Exception {
        sessionOther = CoreInstance.getCoreSession("other");

        ctx = new OperationContext(session);

        documentModel = session.createDocumentModel("/", "src", "Folder");
        documentModel.setPropertyValue("dc:title", "Source");
        documentModel = session.createDocument(documentModel);

        docOther = sessionOther.createDocumentModel("/", "docOther", "File");
        docOther.setPropertyValue("dc:title", "Doc Other");
        docOther = sessionOther.createDocument(docOther);
    }

    @After
    public void tearDown() {
        sessionOther.removeDocument(docOther.getRef());
    }

    @Test
    public void shouldAdaptArrayStringAsStringList() throws Exception {
        String[] value = new String[] { "a", "b", };

        Object result = automationService.getAdaptedValue(ctx, value, StringList.class);
        assertNotNull(result);
        assertTrue(result instanceof StringList);
        StringList list = (StringList) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));

    }

    @Test
    public void shouldAdaptArrayStringAsDocumentModelList() throws Exception {

        String[] value = new String[] { documentModel.getId(), documentModel.getRef().toString() };

        Object result = automationService.getAdaptedValue(ctx, value, DocumentModelList.class);
        assertNotNull(result);
        assertTrue(result instanceof DocumentModelList);
        DocumentModelList list = (DocumentModelList) result;
        assertEquals(2, list.size());
        assertEquals("Source", list.get(0).getTitle());
        assertEquals("Source", list.get(1).getTitle());
    }

    @Test
    public void shouldAdaptNullValue() throws Exception {
        assertNull(automationService.getAdaptedValue(ctx, null, Void.class));
        assertNull(automationService.getAdaptedValue(ctx, null, DocumentModel.class));
    }

    @Test
    public void shouldAdaptContext() throws Exception {
        assertEquals(ctx, automationService.getAdaptedValue(ctx, null, OperationContext.class));
    }

    /**
     * Tests the {@link CollectionToDocModelList} type adapter.
     *
     * @since 2023.5
     */
    @Test
    public void shouldAdaptCollectionAsDocumentModelList() throws OperationException {
        var docId = documentModel.getId();
        var docOtherId = docOther.getId();
        var value = List.of( //
                "/src", // String path
                docId, // String id
                new PathRef("/src"), // DocumentRef path
                new IdRef(docId), // DocumentRef id
                new AbsoluteDocumentRef("test", new PathRef("/src")), // AbsoluteDocumentRef path with default repo
                new AbsoluteDocumentRef("test", new IdRef(docId)), // AbsoluteDocumentRef id with default repo
                documentModel, // DocumentModel
                new AbsoluteDocumentRef("other", new PathRef("/docOther")), // AbsoluteDocumentRef path with other repo
                new AbsoluteDocumentRef("other", new IdRef(docOtherId)) // AbsoluteDocumentRef id with other repo
        );

        var result = automationService.getAdaptedValue(ctx, value, DocumentModelList.class);
        assertNotNull(result);
        assertTrue(result instanceof DocumentModelList);
        DocumentModelList list = (DocumentModelList) result;
        assertEquals(9, list.size());
        for (int i = 0; i < 7; i++) {
            var doc = list.get(i);
            checkDocumentModel(doc, "/src", docId, "test");
        }
        for (int i = 7; i < 9; i++) {
            var doc = list.get(i);
            checkDocumentModel(doc, "/docOther", docOtherId, "other");
        }
    }

    /**
     * Tests {@link TypeAdapterHelper#createDocumentModel(AbsoluteDocumentRef)}.
     *
     * @since 2023.5
     */
    @Test
    public void testCreateDocumentModel() {
        // doc from default repository
        var doc = TypeAdapterHelper.createDocumentModel(new AbsoluteDocumentRef("test", documentModel.getRef()));
        checkDocumentModel(doc, "/src", documentModel.getId(), "test");

        // doc from other repository
        doc = TypeAdapterHelper.createDocumentModel(new AbsoluteDocumentRef("other", docOther.getRef()));
        checkDocumentModel(doc, "/docOther", docOther.getId(), "other");
    }

    protected void checkDocumentModel(DocumentModel doc, String path, String id, String repositoryName) {
        assertEquals(path, doc.getPathAsString());
        assertEquals(id, doc.getId());
        assertEquals(repositoryName, doc.getRepositoryName());
    }

}
