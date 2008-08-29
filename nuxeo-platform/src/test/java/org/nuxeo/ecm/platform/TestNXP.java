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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform;

import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.interfaces.ejb.ECContentRoot;
import org.nuxeo.ecm.platform.interfaces.ejb.ECDomain;
import org.nuxeo.ecm.platform.interfaces.ejb.ECServer;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * @author Razvan Caraghin
 *
 */
public class TestNXP extends NXPInit {
    // if: java.io.InvalidClassException: javax.resource.ResourceException;
    // local
    // class incompatible: stream classdesc serialVersionUID
    // see
    // http://www.jboss.com/index.html?module=bb&op=viewtopic&t=65840&view=previous
    // probably this is because too many opened JCR sessions

    protected final Random random = new Random(new Date().getTime());

    protected ECServer server;

    protected ECDomain domain;

    protected ECContentRoot workspace;

    CoreSession handle;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResource(
                "jndi.properties").openStream());

        server = (ECServer) getInitialContext(properties).lookup(
                "nuxeo/ECServerBean/remote");
        assertNotNull(server);

        domain = (ECDomain) getInitialContext(properties).lookup(
                "nuxeo/ECDomainBean/remote");
        assertNotNull(server);

        workspace = (ECContentRoot) getInitialContext(properties).lookup(
                "nuxeo/ECContentRootBean/remote");
        assertNotNull(server);

        RepositoryLocation repoLoc = new RepositoryLocation("demo");

        handle = CoreInstance.getInstance().open(repoLoc.getName(), null);
        assertNotNull(handle);
    }

    @Override
    protected void tearDown() throws Exception {
        handle.removeChildren(handle.getRootDocument().getRef());
        handle.save();
        server.remove();
        server = null;
        domain.remove();
        domain = null;
        workspace.remove();
        workspace = null;

        super.tearDown();
    }

    protected String _generateUnique() {
        return String.valueOf(random.nextLong());
    }

    protected DocumentModel _createChildDocument(DocumentRef ref,
            DocumentModel childFolder) throws ClientException {
        DocumentModel ret = handle.createDocument(childFolder);

        assertNotNull(ret);
        assertNotNull(ret.getName());
        assertNotNull(ret.getId());
        assertNotNull(ret.getRef());
        assertNotNull(ret.getPathAsString());

        return ret;
    }

    protected static InitialContext getInitialContext(Properties properties)
            throws NamingException {
        return new InitialContext(properties);
    }

    public void testRetrieveRepositoryLocations() {
        List<RepositoryLocation> locations = server
                .getAvailableRepositoryLocations();

        assertNotNull(locations);
        assertEquals(1, locations.size());
        assertNotNull(locations.get(0));
        assertEquals("demo", locations.get(0).getName());
    }

    public void testAuthorizedPrincipals() {
        List<Principal> principals = server.getAuthorizedPrincipals();

        assertNotNull(principals);
        assertEquals(2, principals.size());
        assertNotNull(principals.get(0));
        assertEquals("q", principals.get(0).getName());
        assertNotNull(principals.get(1));
        assertEquals("useradmin", principals.get(1).getName());
    }

    public void testGetDomains() throws Exception {
        DocumentModel root = handle.getRootDocument();
        assertNotNull(root);

        String name = "domain#" + _generateUnique();
        DocumentModel childDomain = new DocumentModelImpl(root.getPathAsString(), name,
                "Domain");
        childDomain = _createChildDocument(root.getRef(), childDomain);

        assertEquals("Domain", childDomain.getType());
        assertEquals(name, childDomain.getName());

        List<DocumentModel> domains = domain.getDomains(handle);

        assertNotNull(domains);
        assertEquals(1, domains.size());

        name = "workspace#" + _generateUnique();
        DocumentModel childWorkspace = new DocumentModelImpl(childDomain
                .getPathAsString(), name, "Workspace");
        childWorkspace = _createChildDocument(childDomain.getRef(),
                childWorkspace);

        assertEquals("Workspace", childWorkspace.getType());
        assertEquals(name, childWorkspace.getName());

        List<DocumentModel> workspaces = workspace.getContentRootChildren("Workspace", childDomain
                .getRef(), handle);
        assertNotNull(workspaces);
        assertEquals(1, workspaces.size());
    }

}
