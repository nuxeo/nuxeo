import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.nuxeo.ecm.client.CannotConnectToServerException;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.DocumentFeed;
import org.nuxeo.ecm.client.QueryEntry;
import org.nuxeo.ecm.client.Repository;
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

    public NewswaveIntegration(String name, URL url) throws CannotInstantiateConnectorException {
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

    public void checkRepositories() throws CannotConnectToServerException {
        Repository[] repositories = managerUnderTest.getRepositories();
        assertEquals(1, repositories.length);
        Repository defaultRepository = managerUnderTest.getDefaultRepository();
        assertEquals("default", defaultRepository.getRepositoryId());
    }
    
    public void checkQueries() throws CannotConnectToServerException {
        Repository defaultRepository = managerUnderTest.getDefaultRepository();
        List<QueryEntry> queries = defaultRepository.getQueries();
        assertTrue(queries.size() > 0);
    }
    
    public void checkFeed() throws CannotConnectToServerException {
        Repository defaultRepository = managerUnderTest.getDefaultRepository();
        List<QueryEntry> queries = defaultRepository.getQueries();
        assertTrue(queries.size() > 0);
        QueryEntry firstQuery = queries.get(0);
        DocumentFeed feed = firstQuery.getFeed();
        assertEquals("Feed Title", feed.getTitle());
    }

    public static void main(String args[]) throws MalformedURLException, CannotInstantiateConnectorException {
        String host = args[0];
        URL baseURL;
        baseURL = new URL("http", host, 8080, "/cmis");
        TestSuite suite= new TestSuite();
        suite.addTest(new NewswaveIntegration("checkRepositories",baseURL));
        suite.addTest(new NewswaveIntegration("checkQueries",baseURL));
        suite.addTest(new NewswaveIntegration("checkFeed", baseURL));
        TestRunner.run(suite);
    }
}
