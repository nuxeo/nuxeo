import java.net.MalformedURLException;
import java.net.URL;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.nuxeo.ecm.client.ConnectorException;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.Repository;
import org.nuxeo.ecm.client.httpclient.HttpClientConnector;
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

    public NewswaveIntegration(URL url) throws CannotInstantiateConnectorException {
        super("noop");
        managerUnderTest = new DefaultContentManager();
        managerUnderTest.init(url, HttpClientConnector.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        testRepositories();
    }
    
    public void noop() {
        ;
    }

    final ContentManager managerUnderTest;

    public void testRepositories() throws ConnectorException {
        Repository[] repositories = managerUnderTest.getRepositories();
        assertEquals(1, repositories.length);
        Repository defaultRepository = managerUnderTest.getDefaultRepository();
        assertEquals("default", defaultRepository.getRepositoryId());
    }

    public static void main(String args[]) throws MalformedURLException, CannotInstantiateConnectorException {
        String host = args[0];
        URL baseURL;
        baseURL = new URL("http", host, 8080, "/cmis");
        TestCase test = new NewswaveIntegration(baseURL); // eugen
        TestRunner.run(test);
    }
}
