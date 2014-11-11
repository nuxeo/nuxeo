/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benjamin JALON
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import com.google.inject.Inject;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

        Object result = automationService.getAdaptedValue(ctx, value,
                StringList.class);
        assertNotNull(result);
        assertTrue(result instanceof StringList);
        StringList list = (StringList) result;
        assertEquals(2, list.size());
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));

    }

    @Test
    public void shouldAdaptArrayStringAsDocumentModelList() throws Exception {

        String[] value = new String[] { documentModel.getId(),
                documentModel.getRef().toString() };

        Object result = automationService.getAdaptedValue(ctx, value,
                DocumentModelList.class);
        assertNotNull(result);
        assertTrue(result instanceof DocumentModelList);
        DocumentModelList list = (DocumentModelList) result;
        assertEquals(2, list.size());
        assertEquals("Source", list.get(0).getTitle());
        assertEquals("Source", list.get(1).getTitle());
    }

}
