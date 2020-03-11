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

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class AdapterTest {

    @Inject
    AutomationService automationService;

    @Inject
    CoreSession session;

    private DocumentModel documentModel;

    private OperationContext ctx;

    @Before
    public void setup() {
        ctx = new OperationContext(session);
    }

    @Before
    public void initRepo() throws Exception {
        ctx = new OperationContext(session);
        documentModel = session.createDocumentModel("/", "src", "Folder");
        documentModel.setPropertyValue("dc:title", "Source");
        documentModel = session.createDocument(documentModel);
        session.save();
        documentModel = session.getDocument(documentModel.getRef());
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
}
