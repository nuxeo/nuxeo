/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.security;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.core.search.SearchTestConstants;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourcesFactory;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.ecm.core.search.transaction.Transactions;
import org.nuxeo.runtime.api.Framework;

/**
 * Integration tests for the search service with the default backend
 * configuration (i.e. Compass based).
 *
 * @author <a href="ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
public class TestSearchPolicy extends RepositoryOSGITestCase {

    // reproducible pseudo-random generator
    private final Random random = new Random(0);

    SearchService sservice;

    @Override
    public void setUp() throws Exception {
        // deploy the default core configurations with schemas and types
        super.setUp();

        // deploy the default search service
        deployBundle("org.nuxeo.ecm.platform.search.api");
        deployBundle("org.nuxeo.ecm.platform.search");
        deployBundle("org.nuxeo.ecm.platform.search.compass-plugin");

        // override the default compass configuration to instead use
        // a non-JXTA memory backend
        deployContrib(SearchTestConstants.SEARCH_INTEGRATION_TEST_BUNDLE,
                "nxsearch-compass-test-integration-contrib.xml");

        deployBundle("org.nuxeo.ecm.platform.search.core.listener");

        // setup specific schemas
        deployContrib(SearchTestConstants.SEARCH_INTEGRATION_TEST_BUNDLE,
                "nxsearch-policy-test-CoreExtensions.xml");

        openRepository();
        sservice = Framework.getService(SearchService.class);
        // set test mode for transactions management within search backend
        Transactions.setTest(true);
    }

    @Override
    protected void tearDown() throws Exception {
        // set test mode for transactions management within search backend
        Transactions.setTest(false);
        super.tearDown();
    }

    private SearchPageProvider query(String query, Principal principal)
            throws Exception {
        SQLQuery nxqlQuery = SQLQueryParser.parse(query);
        ComposedNXQuery composedQuery = new ComposedNXQueryImpl(nxqlQuery);
        if (principal != null) {
            composedQuery.setSearchPrincipal(sservice.getSearchPrincipal(principal));
        }
        ResultSet rs = sservice.searchQuery(composedQuery, 0, 100);
        return new SearchPageProvider(rs);
    }

    private DocumentModel createSampleFile() throws Exception {
        // Create a document model.
        DocumentModel root = coreSession.getRootDocument();
        DocumentModel file = coreSession.createDocumentModel(
                root.getPathAsString(), String.valueOf(random.nextLong()),
                "File");

        // fill in some default values
        file.setPropertyValue("dc:title", "Title of the file");
        file.setPropertyValue("file:filename", "sample-text-file.txt");
        file.setPropertyValue("sp:securityLevel", "3");

        // save it in the repository (and synchronous indexing of the pre-fetch)
        file = coreSession.createDocument(file);
        coreSession.save();

        // simulate asynchronous fulltext indexing
        // sservice.indexInThread(file, false, true);
        sservice.index(IndexableResourcesFactory.computeResourcesFor(file,
                file.getSessionId()), true);

        return file;
    }

    private static DocumentModel getUserModel(Long accessLevel) {
        DocumentModelImpl documentModelImpl = new DocumentModelImpl("User");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("accessLevel", Long.valueOf(3));
        documentModelImpl.addDataModel(new DataModelImpl("user", data));
        return documentModelImpl;
    }

    protected void checkPolicy() throws Exception {
        DocumentRef docRef = createSampleFile().getRef();

        // search for a word occurring in the title field
        String query = "SELECT * FROM Document WHERE ecm:fulltext = 'title'";
        SearchPageProvider spp = query(query, null);
        assertEquals(1, spp.getResultsCount());

        DocumentModel result = spp.getCurrentPage().get(0);
        assertEquals(docRef, result.getRef());

        // Test Security Policy
        assertEquals("3", result.getPropertyValue("sp:securityLevel"));

        CoreSession remote = getCoreSession();

        // put lower access level => too low for this doc
        ((NuxeoPrincipal) remote.getPrincipal()).setModel(getUserModel(Long.valueOf(2)));
        spp = query(query, remote.getPrincipal());
        assertEquals(0, spp.getResultsCount());
    }

    public void testOldSearchPolicy() throws Exception {
        // setup old secu policy
        deployContrib(SearchTestConstants.SEARCH_INTEGRATION_TEST_BUNDLE,
                "nxsearch-old-policy-test-contrib.xml");
        checkPolicy();
    }

    public void testSearchPolicy() throws Exception {
        // setup new secu policy
        deployContrib(SearchTestConstants.SEARCH_INTEGRATION_TEST_BUNDLE,
                "nxsearch-policy-test-contrib.xml");
        checkPolicy();
    }

}
