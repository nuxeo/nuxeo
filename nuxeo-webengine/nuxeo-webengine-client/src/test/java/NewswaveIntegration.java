import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.nuxeo.ecm.client.abdera.DocumentFeedAdapter;
import org.nuxeo.ecm.client.atompub.AtomPubConnector;
import org.nuxeo.ecm.client.impl.CannotInstantiateConnectorException;
import org.nuxeo.ecm.client.impl.DefaultContentManager;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */

/**
 * @author matic
 *
 */
public class NewswaveIntegration extends NXRuntimeTestCase {

    public NewswaveIntegration(String name)
            throws CannotInstantiateConnectorException, MalformedURLException {
        super(name);
        managerUnderTest = new DefaultContentManager();
        managerUnderTest.init(new URL("http","eugen",8080,"/cmis"), AtomPubConnector.class);
    }

    public NewswaveIntegration(String name, URL url)
            throws CannotInstantiateConnectorException {
        super(name);
        managerUnderTest = new DefaultContentManager();
        managerUnderTest.init(url, AtomPubConnector.class);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void noop() {
        ;
    }

    final ContentManager managerUnderTest;

    public void testGetRepositories() throws CannotConnectToServerException {
        Repository[] repositories = managerUnderTest.getRepositories();
        assertEquals(1, repositories.length);
        Repository defaultRepository = managerUnderTest.getDefaultRepository();
        assertEquals("default", defaultRepository.getRepositoryId());
    }

    public void testGetQueries() throws CannotConnectToServerException {
        Repository defaultRepository = managerUnderTest.getDefaultRepository();
        List<QueryEntry> queries = defaultRepository.getQueries();
        assertTrue(queries.size() > 0);
    }

    public void testGetFeed() throws CannotConnectToServerException {
        Repository defaultRepository = managerUnderTest.getDefaultRepository();
        List<QueryEntry> queries = defaultRepository.getQueries();
        assertTrue(queries.size() > 0);
        QueryEntry firstQuery = queries.get(0);
        DocumentFeed feed = firstQuery.getFeed();
        assertEquals("Feed Title", feed.getTitle());
    }

    public void testRefreshFeed() throws CannotConnectToServerException {
        Repository defaultRepository = managerUnderTest.getDefaultRepository();
        List<QueryEntry> queries = defaultRepository.getQueries();
        assertTrue(queries.size() > 0);
        QueryEntry firstQuery = queries.get(0);
        DocumentFeed feed = firstQuery.getFeed();
        DocumentFeed refreshedFeed = feed.refresh();
        assertNull(refreshedFeed);
        DocumentFeedAdapter adapter = (DocumentFeedAdapter)feed;
        String serverTag = adapter.getServerTag();
        Long value = Long.parseLong(serverTag) - 1;
        adapter.setServerTag(value.toString());
        refreshedFeed = feed.refresh();
        assertNotNull(refreshedFeed);
        assertEquals(1, refreshedFeed.size()-feed.size());
    }

    public static void main(String args[]) throws MalformedURLException,
            CannotInstantiateConnectorException {
        String host = args[0];
        URL baseURL;
        baseURL = new URL("http", host, 8080, "/cmis");
        TestSuite suite = new TestSuite();
        suite.addTest(new NewswaveIntegration("testGetRepositories", baseURL));
        suite.addTest(new NewswaveIntegration("testGetQueries", baseURL));
        suite.addTest(new NewswaveIntegration("testGetFeed", baseURL));
        suite.addTest(new NewswaveIntegration("testRefreshFeed", baseURL));
        TestRunner.run(suite);
    }
}
