/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.automation.scripting.internals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nuxeo.automation.scripting.AutomationScriptingFeature;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationScriptingFeature.class)
public class TestAutomationMapper {

    @Inject
    private CoreSession session;

    private AutomationMapper mapper;

    @Before
    public void injectMapper() {
        mapper = new AutomationMapper(new OperationContext(session));
    }

    @Test
    public void testWrap() {
        // Run test
        Object result = DocumentScriptingWrapper.wrap("string", mapper);

        // Assert
        assertEquals("string", result);
    }

    @Test
    public void testWrapDocumentModel() {
        // Init parameters
        DocumentModel doc = mock(DocumentModel.class);

        // Run test
        Object result = DocumentScriptingWrapper.wrap(doc, mapper);

        // Assert
        assertTrue(result instanceof DocumentScriptingWrapper);
        assertEquals(doc, ((DocumentScriptingWrapper) result).getDoc());
    }

    @Test
    public void testWrapDocumentModelList() {
        // Init parameters
        DocumentModelListImpl docList = new DocumentModelListImpl();
        DocumentModel doc = mock(DocumentModel.class);
        docList.add(doc);

        // Run test
        Object result = DocumentScriptingWrapper.wrap(docList, mapper);

        // Assert
        assertTrue(result instanceof List<?>);
        assertTrue(((List<?>) result).get(0) instanceof DocumentScriptingWrapper);
        assertEquals(doc, ((DocumentScriptingWrapper) ((List<?>) result).get(0)).getDoc());
    }

    @Test
    public void testUnwrap() {

    }

    @Test
    public void testUnwrapDocumentScriptingWrapper() {
        DocumentModel doc = new DocumentModelImpl("dummy");
        doc = Mockito.spy(doc);
        Mockito.when(doc.getSchemas()).thenReturn(new String[] { "foo" });
        Map<String,Object> properties = new HashMap<>();
        properties.put("key", "value");
        Mockito.when(doc.getProperties("foo")).thenReturn(properties);

        // Run test
        Map<String,Object> result = DocumentScriptingWrapper.unwrap(new DocumentScriptingWrapper(mapper, doc));

        // Assert
        assertEquals(properties, result);
    }

    @Test
    public void testUnwrapDocumentModelList() {
        // Run test
        Object result = DocumentScriptingWrapper.unwrap(new DocumentModelListImpl());

        // Assert
        assertTrue(result instanceof DocumentModelListImpl);
    }

    @Test
    public void testUnwrapBlobList() {
        // Run test
        Object result = DocumentScriptingWrapper.unwrap(new BlobList());

        // Assert
        assertTrue(result instanceof BlobList);
    }

    @Test
    public void testUnwrapDocumentModelInList() {
        // Init parameters
        List<DocumentModel> docList = new ArrayList<>();
        DocumentModel doc = mock(DocumentModel.class);
        docList.add(doc);

        // Run test
        Object result = DocumentScriptingWrapper.unwrap(docList);

        // Assert
        assertTrue(result instanceof DocumentModelList);
        assertEquals(doc, ((DocumentModelList) result).get(0));
    }

    @Test
    public void testUnwrapBlobInList() {
        // Init parameters
        List<Blob> blobList = new ArrayList<>();
        Blob blob = mock(Blob.class);
        blobList.add(blob);

        // Run test
        Object result = DocumentScriptingWrapper.unwrap(blobList);

        // Assert
        assertTrue(result instanceof BlobList);
        assertEquals(blob, ((BlobList) result).get(0));
    }

    @Test
    public void testUnwrapDocumentScriptingWrapperInList() {
        // Init parameters
        List<DocumentScriptingWrapper> docList = new ArrayList<>();
        DocumentModel doc = mock(DocumentModel.class);
        docList.add(new DocumentScriptingWrapper(mapper, doc));

        // Run test
        Object result = DocumentScriptingWrapper.unwrap(docList);

        // Assert
        assertTrue(result instanceof DocumentModelList);
        assertEquals(doc, ((DocumentModelList) result).get(0));
    }

    @Test
    public void testBijunction() {

    }

}
