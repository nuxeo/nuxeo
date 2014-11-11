/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.FieldImpl;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.ResultItemImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.ResultSetImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 * @deprecated use content views instead
 */
@Deprecated
public class TestSearchPageProvider extends NXRuntimeTestCase {

    @SuppressWarnings("serial")
    static class MockSearchPageProvider extends SearchPageProvider {

        private MockSearchPageProvider(ResultSet set) {
            super(set);
        }

        @Override
        protected String getSchemaByPrefix(String prefix) {
            if ("file".equals(prefix)) {
                return "file";
            } else if ("dc".equals(prefix)) {
                return "dublincore";
            } else {
                return null;
            }
        }

        @Override
        protected Field getSchemaField(String schemaName, String fieldName) {
            // XXX: OG I am too lazy to mock complex types here
            // using a mocked simple type field while disable the blobFilter
            // method from working correctly with complex fields thus we cannot
            // test them here. However this is done in the search service
            // integration test suite
            QName qname = new QName(schemaName, fieldName);
            return new FieldImpl(qname, StringType.INSTANCE,
                    StringType.INSTANCE);
        }
    }

    private ResultItem resultItem;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        resultItem = new ResultItemImpl(buildResultItemMap(), "the_id");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core");
    }

    private static Map<String, Serializable> buildResultItemMap() {
        Map<String, Serializable> res = new HashMap<String, Serializable>();

        DocumentRef docRef = new PathRef("doc/path");
        DocumentRef parentRef = new IdRef("some_id");

        res.put(BuiltinDocumentFields.FIELD_DOC_REF, docRef);
        res.put(BuiltinDocumentFields.FIELD_DOC_REPOSITORY_NAME, "zerepo");
        res.put(BuiltinDocumentFields.FIELD_DOC_PARENT_REF, parentRef);
        res.put(BuiltinDocumentFields.FIELD_DOC_PATH, "doc/path");
        res.put(BuiltinDocumentFields.FIELD_DOC_TYPE, "Workspace");
        res.put(BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE, "submitted");
        res.put(BuiltinDocumentFields.FIELD_DOC_VERSION_LABEL, "00.1");

        res.put("dc:title", "the_title");
        res.put("file:filename", "bulgroz");

        return res;
    }

    public void testGetNumberOfPages() {
        ResultSet set = new ResultSetImpl((SQLQuery) null, null, 0, 10,
                Collections.<ResultItem>emptyList(), 17, 10);
        SearchPageProvider provider = new SearchPageProvider(set);
        assertEquals(2, provider.getNumberOfPages());
    }

    public void testGetCurerntPageSize() {
        ResultSet set = new ResultSetImpl((SQLQuery) null, null, 10, 10,
                Collections.<ResultItem>emptyList(), 17, 7);
        SearchPageProvider provider = new SearchPageProvider(set);
        assertEquals(7, provider.getCurrentPageSize());
    }

    // A result item that lacks docRef
    private static ResultItem corruptedResultItem1() {
        Map<String, Serializable> res = buildResultItemMap();
        res.remove(BuiltinDocumentFields.FIELD_DOC_REF);
        return new ResultItemImpl(res, "the_id");
    }

    /**
     * Checks that a DocumentModel constructed from member field resultItem is
     * correct.
     */
    public static void checkSetUpDocumentModel(DocumentModel docModel)
            throws Exception {
        assertEquals(new PathRef("doc/path"), docModel.getRef());
        assertEquals("Workspace", docModel.getType());
        assertEquals("the_title", docModel.getProperty("dublincore", "title"));
        assertEquals("bulgroz", docModel.getProperty("file", "filename"));
        assertEquals("submitted", docModel.getCurrentLifeCycleState());
        assertEquals("zerepo", docModel.getRepositoryName());
        assertEquals("00.1", docModel.getVersionLabel());
    }

    public void testGetCurrentPage() throws Exception {
        SearchPageProvider provider = new MockSearchPageProvider(
                new ResultSetImpl((SQLQuery) null, null, 14, 20,
                        Arrays.asList(resultItem), 16, 1));
        DocumentModelList docModels = provider.getCurrentPage();
        assertEquals(1, docModels.size());
        checkSetUpDocumentModel(docModels.get(0));
    }

    public void testGetCurrentPageWithCorrupted() throws Exception {
        SearchPageProvider provider = new MockSearchPageProvider(
                new ResultSetImpl((SQLQuery) null, null, 14, 20, Arrays.asList(
                        resultItem, corruptedResultItem1()), 17, 2));
        DocumentModelList docModels = provider.getCurrentPage();
        assertEquals(1, docModels.size());
        checkSetUpDocumentModel(docModels.get(0));
    }

    /*
     * See NXP-1696
     */
    public void testEmptyResults() {
        SearchPageProvider provider = new SearchPageProvider(
                new ResultSetImpl((SQLQuery) null, null, 0, 10, Collections.<ResultItem>emptyList(), 0, 0));
        assertFalse(provider.isNextPageAvailable());
        assertFalse(provider.isPreviousPageAvailable());
        assertEquals(0, provider.getCurrentPageIndex());
        assertEquals(0, provider.getCurrentPageOffset());
        assertEquals(0, provider.getCurrentPageSize());

        provider.rewind();
        provider.last();
        // GR this could be discussed. I mostly want to
        // check that no exception is thrown
        assertEquals(1, provider.getNumberOfPages());
    }

}
