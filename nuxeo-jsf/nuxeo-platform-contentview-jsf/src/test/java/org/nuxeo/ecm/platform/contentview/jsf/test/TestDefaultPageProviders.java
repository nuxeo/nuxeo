/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf.test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageSelections;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.ui.web.jsf.MockFacesContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.junit.Assume.assumeTrue;

/**
 * @author Anahide Tchertchian
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.query.api", //
        "org.nuxeo.ecm.platform.contentview.jsf", //
        "org.nuxeo.ecm.core.io"
})
@LocalDeploy("org.nuxeo.ecm.platform.contentview.jsf.test:test-contentview-contrib.xml")
public class TestDefaultPageProviders {

    protected static final Log log = LogFactory.getLog(TestDefaultPageProviders.class);

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    ContentViewService service;

    @Inject
    protected CoreSession coreSession;

    // user "bob"
    protected CoreSession session;

    MockFacesContext facesContext;

    DocumentModel searchDocument;

    String dummyParam = UUID.randomUUID().toString();

    Boolean booleanParam = Boolean.FALSE;

    List<String> listParam = Arrays.asList(new String[] { "deleted", "validated" });

    @Before
    public void setUp() throws Exception {
        // set rights to user "bob" on root
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acl.add(new ACE("bob", SecurityConstants.EVERYTHING, true));
        acp.addACL(acl);
        coreSession.setACP(coreSession.getRootDocument().getRef(), acp, false);
        coreSession.save();

        session = CoreInstance.openCoreSession(coreSession.getRepositoryName(), "bob");

        searchDocument = session.createDocumentModel("File");

        final DocumentModel rootDoc = session.getRootDocument();

        // set mock faces context for needed properties resolution
        facesContext = new MockFacesContext();
        facesContext.mapExpression("#{dummy.param}", dummyParam);
        facesContext.mapVariable("documentManager", session);
        facesContext.mapVariable("searchDocument", searchDocument);
        facesContext.mapVariable("currentDocument", rootDoc);
        facesContext.mapVariable("booleanParam", booleanParam);
        facesContext.mapVariable("listParam", listParam);
        facesContext.setCurrent();
        assertNotNull(FacesContext.getCurrentInstance());

        service = Framework.getService(ContentViewService.class);
        assertNotNull(service);

        createTestDocuments();
    }

    @After
    public void tearDown() {
        if (session != null) {
            session.close();
        }
        if (facesContext != null) {
            facesContext.relieveCurrent();
        }
    }

    protected void createTestDocuments() {
        final DocumentModel root = session.getRootDocument();
        // create docs in descending order so that docs are not ordered by
        // title by default
        for (int i = 4; i >= 0; i--) {
            DocumentModel doc = session.createDocumentModel("Folder");
            doc.setPropertyValue("dc:title", "Document number" + i); // no
                                                                     // space
            doc.setPathInfo(root.getPathAsString(), "doc_" + i);
            session.createDocument(doc);
        }
        // also create another document as unrestricted, so that Administrator
        // does not have access to it by default
        new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                DocumentModel doc = session.createDocumentModel("Folder");
                doc.setPropertyValue("dc:title", "Document restricted");
                doc.setPathInfo(root.getPathAsString(), "doc_restricted");
                doc = session.createDocument(doc);
                // set restriction
                ACP acp = new ACPImpl();
                ACL acl = new ACLImpl();
                acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
                acp.addACL(acl);
                session.setACP(doc.getRef(), acp, true);
            }
        }.runUnrestricted();
        session.save();
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQuery() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        assertEquals(parentIdParam, contentView.getCacheKey());

        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(
                parentIdParam, booleanParam, dummyParam, listParam);
        checkCoreQuery(parentIdParam, pp);

        // check page size options
        List<Long> options = pp.getPageSizeOptions();
        assertEquals(4, options.size());
        assertEquals(2L, options.get(0).longValue());
        assertEquals(10L, options.get(1).longValue());
        assertEquals(15L, options.get(2).longValue());
        assertEquals(20L, options.get(3).longValue());
        pp.setPageSize(100);
        assertEquals(100, pp.getPageSize());
        options = pp.getPageSizeOptions();
        assertEquals(5, options.size());
        assertEquals(2L, options.get(0).longValue());
        assertEquals(10L, options.get(1).longValue());
        assertEquals(15L, options.get(2).longValue());
        assertEquals(20L, options.get(3).longValue());
        assertEquals(100L, options.get(4).longValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCoreQueryReference() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_REF");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        assertEquals(parentIdParam, contentView.getCacheKey());

        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(
                parentIdParam, booleanParam, dummyParam, listParam);
        checkCoreQuery(parentIdParam, pp);

        // check page size options
        List<Long> options = pp.getPageSizeOptions();
        assertEquals(7, options.size());
        assertEquals(7, options.size());
        assertEquals(2L, options.get(0).longValue());
        assertEquals(5L, options.get(1).longValue());
        assertEquals(10L, options.get(2).longValue());
        assertEquals(20L, options.get(3).longValue());
        assertEquals(30L, options.get(4).longValue());
        assertEquals(40L, options.get(5).longValue());
        assertEquals(50L, options.get(6).longValue());
        pp.setPageSize(17);
        assertEquals(17, pp.getPageSize());
        options = pp.getPageSizeOptions();
        assertEquals(8, options.size());
        assertEquals(2L, options.get(0).longValue());
        assertEquals(5L, options.get(1).longValue());
        assertEquals(10L, options.get(2).longValue());
        assertEquals(17L, options.get(3).longValue());
        assertEquals(20L, options.get(4).longValue());
        assertEquals(30L, options.get(5).longValue());
        assertEquals(40L, options.get(6).longValue());
        assertEquals(50L, options.get(7).longValue());
    }

    protected void checkCoreQuery(String parentIdParam, PageProvider<DocumentModel> pp) throws Exception {

        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<DocumentModel> docs = pp.getCurrentPage();

        // check query
        assertTrue(pp instanceof CoreQueryDocumentPageProvider);
        assertEquals(String.format("SELECT * FROM Document WHERE ecm:parentId = '%s'"
                + " AND ecm:isCheckedInVersion = 0 AND ecm:mixinType != 'HiddenInNavigation'"
                + " AND ecm:parentId != '%s' AND ecm:currentLifeCycleState NOT IN ('deleted', 'validated')"
                + " ORDER BY dc:title", parentIdParam, dummyParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertFalse(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number0", docs.get(0).getPropertyValue("dc:title"));
        assertEquals("Document number1", docs.get(1).getPropertyValue("dc:title"));

        pp.nextPage();
        docs = pp.getCurrentPage();

        assertEquals(String.format("SELECT * FROM Document WHERE ecm:parentId = '%s'"
                + " AND ecm:isCheckedInVersion = 0 AND ecm:mixinType != 'HiddenInNavigation'"
                + " AND ecm:parentId != '%s' AND ecm:currentLifeCycleState NOT IN ('deleted', 'validated')"
                + " ORDER BY dc:title", parentIdParam, dummyParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number2", docs.get(0).getPropertyValue("dc:title"));
        assertEquals("Document number3", docs.get(1).getPropertyValue("dc:title"));

        // test selection
        pp.setSelectedEntries(Arrays.asList(new DocumentModel[] { docs.get(1) }));
        PageSelections<DocumentModel> selections = pp.getCurrentSelectPage();
        assertNotNull(selections);
        assertEquals(2, selections.getSize());
        assertFalse(selections.isSelected());
        assertEquals("Document number2", selections.getEntries().get(0).getData().getPropertyValue("dc:title"));
        assertFalse(selections.getEntries().get(0).isSelected());
        assertEquals("Document number3", selections.getEntries().get(1).getData().getPropertyValue("dc:title"));
        assertTrue(selections.getEntries().get(1).isSelected());
    }

    // same test but taking XML parameters instead of passing them through API
    // calls
    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithXMLParameters() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProvider();
        checkCoreQueryWithXMLParameter(parentIdParam, pp);

        // Test that a new page provider is returned when one of the parameter
        // changes.
        // In this case, it's dummy.param that changes.
        facesContext.mapExpression("#{dummy.param}", UUID.randomUUID().toString());
        PageProvider<DocumentModel> pp2 = (PageProvider<DocumentModel>) contentView.getPageProvider();
        assertTrue(!pp2.equals(pp));
        assertTrue(!pp2.getParameters()[2].equals(pp.getParameters()[2]));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithXMLParametersReference() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_REF");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProvider();
        checkCoreQueryWithXMLParameter(parentIdParam, pp);
    }

    protected void checkCoreQueryWithXMLParameter(String parentIdParam, PageProvider<DocumentModel> pp)
            throws Exception {
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<DocumentModel> docs = pp.getCurrentPage();

        // check query
        assertTrue(pp instanceof CoreQueryDocumentPageProvider);
        assertEquals(String.format("SELECT * FROM Document WHERE ecm:parentId = '%s'"
                + " AND ecm:isCheckedInVersion = 0 AND ecm:mixinType != 'HiddenInNavigation'"
                + " AND ecm:parentId != '%s' AND ecm:currentLifeCycleState NOT IN ('deleted', 'validated')"
                + " ORDER BY dc:title", parentIdParam, dummyParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertFalse(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number0", docs.get(0).getPropertyValue("dc:title"));
        assertEquals("Document number1", docs.get(1).getPropertyValue("dc:title"));

        pp.nextPage();
        docs = pp.getCurrentPage();

        assertEquals(String.format("SELECT * FROM Document WHERE ecm:parentId = '%s'"
                + " AND ecm:isCheckedInVersion = 0 AND ecm:mixinType != 'HiddenInNavigation'"
                + " AND ecm:parentId != '%s' AND ecm:currentLifeCycleState NOT IN ('deleted', 'validated')"
                + " ORDER BY dc:title", parentIdParam, dummyParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number2", docs.get(0).getPropertyValue("dc:title"));
        assertEquals("Document number3", docs.get(1).getPropertyValue("dc:title"));

        // test selection
        pp.setSelectedEntries(Arrays.asList(new DocumentModel[] { docs.get(1) }));
        PageSelections<DocumentModel> selections = pp.getCurrentSelectPage();
        assertNotNull(selections);
        assertEquals(2, selections.getSize());
        assertFalse(selections.isSelected());
        assertEquals("Document number2", selections.getEntries().get(0).getData().getPropertyValue("dc:title"));
        assertFalse(selections.getEntries().get(0).isSelected());
        assertEquals("Document number3", selections.getEntries().get(1).getData().getPropertyValue("dc:title"));
        assertTrue(selections.getEntries().get(1).isSelected());
    }

    @Test
    public void testCoreQueryMaxResults() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        assertEquals(parentIdParam, contentView.getCacheKey());

        CoreQueryDocumentPageProvider pp = (CoreQueryDocumentPageProvider) contentView.getPageProviderWithParams(
                parentIdParam, booleanParam, dummyParam, listParam);
        pp.setMaxResults(2);
        assertEquals(2, pp.getPageSize());

        // page 1/3
        List<DocumentModel> docs = pp.getCurrentPage();
        assertEquals(PageProvider.UNKNOWN_SIZE_AFTER_QUERY, pp.getResultsCount());

        assertFalse(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertEquals(0, pp.getNumberOfPages());

        assertNotNull(docs);
        assertEquals(2, docs.size());

        // page 2/3
        pp.nextPage();
        docs = pp.getCurrentPage();
        assertTrue(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());

        // page 3/3
        pp.nextPage();
        docs = pp.getCurrentPage();
        assertTrue(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        // last page detected because there are less than pagesize docs
        assertFalse(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(1, docs.size());

        // page 4/3
        // dummy case
        pp.nextPage();
        docs = pp.getCurrentPage();
        assertTrue(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        assertFalse(pp.isNextPageAvailable());
        assertNotNull(docs);
        assertEquals(0, docs.size());

        // borderline case
        pp.setPageSize(5);
        pp.setMaxResults(3);
        pp.firstPage();
        pp.refresh();

        // page 1/2
        docs = pp.getCurrentPage();
        assertEquals(PageProvider.UNKNOWN_SIZE_AFTER_QUERY, pp.getResultsCount());

        assertFalse(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        // correct because we don't know if there is another page
        assertTrue(pp.isNextPageAvailable());

        assertEquals(0, pp.getNumberOfPages());
        assertNotNull(docs);
        assertEquals(5, docs.size());

        // page 2/2
        // there is no more results, user see an empty page
        pp.nextPage();
        docs = pp.getCurrentPage();
        assertTrue(pp.isPreviousPageAvailable());
        assertFalse(pp.isLastPageAvailable());
        assertFalse(pp.isNextPageAvailable());
        assertNotNull(docs);
        assertEquals(0, docs.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryAndFetch() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_FETCH");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryAndFetch(parentIdParam, pp);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryAndFetchReference() throws Exception {
        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_FETCH_REF");
        assertNotNull(contentView);

        String parentIdParam = session.getRootDocument().getId();
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryAndFetch(parentIdParam, pp);
    }

    protected void checkCoreQueryAndFetch(String parentIdParam, PageProvider<Map<String, Serializable>> pp)
            throws Exception {
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<Map<String, Serializable>> docs = pp.getCurrentPage();

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number0", docs.get(0).get("dc:title"));
        assertEquals("Document number1", docs.get(1).get("dc:title"));

        // check query
        assertTrue(pp instanceof CoreQueryAndFetchPageProvider);
        assertEquals(String.format("SELECT dc:title FROM Document WHERE ecm:parentId = '%s'"
                + " AND ecm:isCheckedInVersion = 0" + " AND ecm:mixinType != 'HiddenInNavigation'"
                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title", parentIdParam),
                ((CoreQueryAndFetchPageProvider) pp).getCurrentQuery());

        pp.nextPage();
        docs = pp.getCurrentPage();

        assertEquals(String.format("SELECT dc:title FROM Document WHERE ecm:parentId = '%s'"
                + " AND ecm:isCheckedInVersion = 0" + " AND ecm:mixinType != 'HiddenInNavigation'"
                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title", parentIdParam),
                ((CoreQueryAndFetchPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertTrue(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number2", docs.get(0).get("dc:title"));
        assertEquals("Document number3", docs.get(1).get("dc:title"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryAndFetchWithError() throws Exception {

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_FETCH");
        assertNotNull(contentView);

        // do not pass params => query will not be built correctly
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) contentView.getPageProvider();
        checkCoreQueryAndFetchWithError(pp);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryAndFetchWithErrorReference() throws Exception {

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_FETCH_REF");
        assertNotNull(contentView);

        // do not pass params => query will not be built correctly
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) contentView.getPageProvider();
        checkCoreQueryAndFetchWithError(pp);
    }

    protected void checkCoreQueryAndFetchWithError(PageProvider<Map<String, Serializable>> pp) throws Exception {
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());
        assertNull(pp.getError());
        assertNull(pp.getErrorMessage());

        // init results
        pp.getCurrentPage();

        assertEquals(-1, pp.getResultsCount());
        assertNotNull(pp.getError());
        assertEquals(
                "Failed to execute query: NXQL: SELECT dc:title FROM Document WHERE ecm:parentId = ORDER BY dc:title, "
                        + "Syntax error: Invalid token <ORDER BY> at offset 51", pp.getErrorMessage());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithSearchDocument() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().supportsMultipleFulltextIndexes());

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(contentView);

        // leave default values on doc for now: will filter on all docs with
        // given parent
        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryWithSearchDocument(parentIdParam, pp);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithSimpleDocumentModel() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().supportsMultipleFulltextIndexes());

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SIMPLE_DOC_MODEL");
        assertNotNull(contentView);

        // leave default values on doc for now: will filter on all docs with
        // given parent
        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(parentIdParam);
        pp.setSearchDocumentModel(new SimpleDocumentModel());
        checkCoreQueryWithSimpleDocumentModel(parentIdParam, pp);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCoreQueryWithSearchDocumentReference() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().supportsMultipleFulltextIndexes());

        ContentView contentView = service.getContentView("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT_REF");
        assertNotNull(contentView);

        // leave default values on doc for now: will filter on all docs with
        // given parent
        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryWithSearchDocument(parentIdParam, pp);
    }

    protected void checkCoreQueryWithSimpleDocumentModel(String parentIdParam, PageProvider<DocumentModel> pp)
            throws Exception {
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<DocumentModel> docs = pp.getCurrentPage();

        // check query
        assertTrue(pp instanceof CoreQueryDocumentPageProvider);
        assertEquals(String.format("SELECT * FROM Folder WHERE ecm:parentId = '%s'" + " AND ecm:isCheckedInVersion = 0"
                        + " AND ecm:mixinType != 'HiddenInNavigation'"
                        + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title", parentIdParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertFalse(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number0", docs.get(0).getPropertyValue("dc:title"));
        assertEquals("Document number1", docs.get(1).getPropertyValue("dc:title"));

    }

    protected void checkCoreQueryWithSearchDocument(String parentIdParam, PageProvider<DocumentModel> pp)
            throws Exception {
        assertNotNull(pp);

        assertEquals(-1, pp.getResultsCount());
        assertEquals(0, pp.getNumberOfPages());

        // init results
        List<DocumentModel> docs = pp.getCurrentPage();

        // check query
        assertTrue(pp instanceof CoreQueryDocumentPageProvider);
        assertEquals(String.format("SELECT * FROM Folder WHERE ecm:parentId = '%s'" + " AND ecm:isCheckedInVersion = 0"
                + " AND ecm:mixinType != 'HiddenInNavigation'"
                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title", parentIdParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

        assertEquals(5, pp.getResultsCount());
        assertEquals(3, pp.getNumberOfPages());
        assertFalse(pp.isPreviousPageAvailable());
        assertTrue(pp.isNextPageAvailable());

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertEquals("Document number0", docs.get(0).getPropertyValue("dc:title"));
        assertEquals("Document number1", docs.get(1).getPropertyValue("dc:title"));

        // fill search document with some properDocumentModelties
        searchDocument.setPropertyValue("dc:title", "number0");

        docs = pp.getCurrentPage();

//        assertEquals(String.format("SELECT * FROM Folder WHERE ecm:fulltext.dc:title = 'number0'"
//                + " AND (ecm:parentId = '%s'" + " AND ecm:isCheckedInVersion = 0"
//                + " AND ecm:mixinType != 'HiddenInNavigation'"
//                + " AND ecm:currentLifeCycleState != 'deleted') ORDER BY dc:title", parentIdParam),
//                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());

//        assertEquals(1, pp.getResultsCount());
//        assertEquals(1, pp.getNumberOfPages());
//        assertFalse(pp.isPreviousPageAvailable());
//        assertFalse(pp.isNextPageAvailable());
//
//        assertNotNull(docs);
//        assertEquals(1, docs.size());
//        assertEquals("Document number0", docs.get(0).getPropertyValue("dc:title"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCoreQueryWithSearchDocumentWithWhereClause() throws Exception {
        ContentView contentView = service.getContentView("QUERY_WITH_SUBCLAUSE");
        assertNotNull(contentView);

        // leave default values on doc for now: will filter on all docs with
        // given parent
        String parentIdParam = session.getRootDocument().getId();
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProviderWithParams(parentIdParam);
        checkCoreQueryWithSearchDocumentWithWhereClause(parentIdParam, pp);
    }

    protected void checkCoreQueryWithSearchDocumentWithWhereClause(String parentIdParam, PageProvider<DocumentModel> pp)
            throws Exception {
        // init results
        pp.getCurrentPage();

        // check query
        assertTrue(pp instanceof CoreQueryDocumentPageProvider);
        assertEquals(String.format("SELECT * FROM Document WHERE ecm:parentId = '%s'"
                + " AND ecm:isCheckedInVersion = 0" + " AND ecm:mixinType != 'HiddenInNavigation'"
                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:title", parentIdParam),
                ((CoreQueryDocumentPageProvider) pp).getCurrentQuery());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCoreQueryUnrestricted() throws Exception {
        ContentView contentView = service.getContentView("QUERY_RESTRICTED");
        assertNotNull(contentView);
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) contentView.getPageProvider();
        List<DocumentModel> docs = pp.getCurrentPage();
        assertEquals(5, docs.size());

        contentView = service.getContentView("QUERY_UNRESTRICTED");
        assertNotNull(contentView);
        pp = (PageProvider<DocumentModel>) contentView.getPageProvider();
        docs = pp.getCurrentPage();
        // there's one more
        assertEquals(6, docs.size());
        // check docs are detached
        checkDetached(docs, true);

        contentView = service.getContentView("QUERY_UNRESTRICTED_NO_DETACH");
        assertNotNull(contentView);
        pp = (PageProvider<DocumentModel>) contentView.getPageProvider();
        docs = pp.getCurrentPage();
        // there's still six results
        assertEquals(6, docs.size());
        // check docs are not detached
        checkDetached(docs, false);
    }

    protected void checkDetached(List<DocumentModel> docs, boolean shouldBeDetached) {
        for (DocumentModel doc : docs) {
            if ("doc_restricted".equals(doc.getName())) {
                boolean isDetached = doc.getSessionId() == null;
                assertTrue(shouldBeDetached == isDetached);
                break;
            }
        }
    }

    @Test
    public void testCoreQueryWithMaxResults() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (AbstractSession) session);
        PageProvider<?> pp = pps.getPageProvider("CURRENT_DOCUMENT_MAX_PAGE_SIZE", (DocumentModel) null, null, null,
                null, props);

        assertNotNull(pp);

        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(2, p.size());
        assertEquals(5, pp.getResultsCount());
        assertEquals(100, pp.getPageSize());
        assertEquals(2, pp.getMaxPageSize());
        assertEquals(3, pp.getNumberOfPages());
    }

    @Test
    public void testWaitForExecutionPP() throws Exception {
        ContentView cv = service.getContentView("NAMED_PAGE_PROVIDER");
        assertNotNull(cv);
        assertTrue(cv.isWaitForExecution());
        assertFalse(cv.isExecuted());
        PageProvider<?> pp = cv.getPageProvider();
        assertNull(pp);
        cv.refreshPageProvider();
        assertTrue(cv.isWaitForExecution());
        assertTrue(cv.isExecuted());
        pp = cv.getPageProvider();
        assertNotNull(pp);

        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(5, p.size());
        assertEquals(5, pp.getResultsCount());
        assertEquals(0, pp.getPageSize());
        assertEquals(1000, pp.getMaxPageSize());
        assertEquals(1, pp.getNumberOfPages());
    }

}
