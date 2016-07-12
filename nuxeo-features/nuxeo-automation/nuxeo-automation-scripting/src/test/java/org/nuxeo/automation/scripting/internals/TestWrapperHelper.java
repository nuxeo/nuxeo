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
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

public class TestWrapperHelper {

    private CoreSession session = mock(CoreSession.class);

    @Test
    public void testWrap() {
        // Run test
        Object result = WrapperHelper.wrap("string", session);

        // Assert
        assertEquals("string", result);
    }

    @Test
    public void testWrapDocumentModel() {
        // Init parameters
        DocumentModel doc = mock(DocumentModel.class);

        // Run test
        Object result = WrapperHelper.wrap(doc, session);

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
        Object result = WrapperHelper.wrap(docList, session);

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
        // Init parameters
        DocumentModel doc = mock(DocumentModel.class);

        // Run test
        Object result = WrapperHelper.unwrap(new DocumentScriptingWrapper(session, doc));

        // Assert
        assertEquals(doc, result);
    }

    @Test
    public void testUnwrapDocumentModelList() {
        // Run test
        Object result = WrapperHelper.unwrap(new DocumentModelListImpl());

        // Assert
        assertTrue(result instanceof DocumentModelListImpl);
    }

    @Test
    public void testUnwrapBlobList() {
        // Run test
        Object result = WrapperHelper.unwrap(new BlobList());

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
        Object result = WrapperHelper.unwrap(docList);

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
        Object result = WrapperHelper.unwrap(blobList);

        // Assert
        assertTrue(result instanceof BlobList);
        assertEquals(blob, ((BlobList) result).get(0));
    }

    @Test
    public void testUnwrapDocumentScriptingWrapperInList() {
        // Init parameters
        List<DocumentScriptingWrapper> docList = new ArrayList<>();
        DocumentModel doc = mock(DocumentModel.class);
        docList.add(new DocumentScriptingWrapper(session, doc));

        // Run test
        Object result = WrapperHelper.unwrap(docList);

        // Assert
        assertTrue(result instanceof DocumentModelList);
        assertEquals(doc, ((DocumentModelList) result).get(0));
    }

    @Test
    public void testBijunction() {

    }

}
