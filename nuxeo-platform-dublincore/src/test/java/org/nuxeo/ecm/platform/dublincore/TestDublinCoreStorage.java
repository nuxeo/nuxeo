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

package org.nuxeo.ecm.platform.dublincore;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.platform.dublincore.service.DublinCoreStorageService;

/**
 * DublinCoreStorage Test Case.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class TestDublinCoreStorage extends RepositoryTestCase {

    private DocumentModel root;

    private CoreSession session;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreService.xml");

        deployContrib("org.nuxeo.ecm.platform.dublincore.tests",
                "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.dublincore.tests",
                "LifeCycleService.xml");

        deployContrib("org.nuxeo.ecm.platform.dublincore",
                "OSGI-INF/nxdublincore-service.xml");

        deployBundle("org.nuxeo.ecm.core.event");

        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", "Administrator");
        session = CoreInstance.getInstance().open("demo", context);
        assertNotNull(session);

        root = session.getRootDocument();
    }

    public void testStorageService() {
        DublinCoreStorageService service = NXDublinCore.getDublinCoreStorageService();
        assertNotNull(service);
    }

    public void testCreationDate() throws ClientException {
        DocumentModel childFile = new DocumentModelImpl(
                root.getPathAsString(), "file-007", "File");
        DocumentModel childFile2 = session.createDocument(childFile);

        DataModel dm = childFile2.getDataModel("dublincore");
        assertNotNull(dm.getData("created"));

        DataModel dm2 = session.getDataModel(childFile2.getRef(), "dublincore");
        assertNotNull(dm2.getData("created"));

        assertEquals("Administrator", (String) dm.getData("creator"));
    }

    public void testCreator() throws ClientException {
        DocumentModel childFile = new DocumentModelImpl(
                root.getPathAsString(), "file-007", "File");
        DocumentModel childFile2 = session.createDocument(childFile);

        DataModel dm = childFile2.getDataModel("dublincore");
        assertEquals("Administrator", (String) dm.getData("creator"));

        DataModel dm2 = session.getDataModel(childFile2.getRef(), "dublincore");
        assertEquals("Administrator", (String) dm2.getData("creator"));

        childFile2.setProperty("dublincore", "creator", "toto");
        assertEquals("toto", (String) dm.getData("creator"));

        dm2 = session.getDataModel(childFile2.getRef(), "dublincore");
        assertEquals("Administrator", (String) dm2.getData("creator"));
    }

    public void testModificationDate() throws ClientException {
        DocumentModel childFile = new DocumentModelImpl(
                root.getPathAsString(), "file-008", "File");
        DocumentModel childFile2 = session.createDocument(childFile);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        childFile2.setProperty("dublincore", "title", "toto");

        session.saveDocument(childFile2);

        DataModel dm = childFile2.getDataModel("dublincore");
        Calendar created = (Calendar) dm.getData("created");
        assertNotNull(created);

        DataModel dm2 = session.getDataModel(childFile2.getRef(), "dublincore");
        Calendar modified = (Calendar) dm2.getData("modified");
        assertNotNull(modified);

        assertTrue(modified.getTime() + " !> " + created.getTime(),
                modified.after(created));
    }

    // Wait until we can have a real list management
    public void testContributors() throws ClientException {
        DocumentModel childFile = new DocumentModelImpl(
                root.getPathAsString(), "file-008", "File");
        DocumentModel childFile2 = session.createDocument(childFile);
        DataModel dm = childFile2.getDataModel("dublincore");

        String author = (String) dm.getData("creator");
        assertEquals("Administrator", author);

        String[] contributorsArray = (String[]) dm.getData("contributors");
        List<String> contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Administrator"));

        // modify security to test with a new user

        ACP acp = root.getACP();
        ACL[] acls = acp.getACLs();
        ACL theAcl = acls[0];
        ACE ace = new ACE("Jacky", SecurityConstants.EVERYTHING, true);
        theAcl.add(ace);
        root.setACP(acp, true);

        // create a new session
        session.save();
        session.disconnect();
        session = null;

        Map<String, Serializable> context = new HashMap<String, Serializable>();
        // UserPrincipal newUser = new UserPrincipal("Jacky");
        // newUser.groups.add(SecurityService.ADMINISTRATORS);
        // context.put("username", newUser);
        // switch user in session
        // LocalSession local = (LocalSession) session;
        // local.setPrincipal(newUser);
        context.put("username", "Jacky");
        session = CoreInstance.getInstance().open("demo", context);

        DocumentModel childFile3 = session.getDocument(childFile2.getRef());
        childFile3.setProperty("dublincore", "source", "testing");
        childFile3 = session.saveDocument(childFile3);

        contributorsArray = (String[]) childFile3.getDataModel("dublincore").getData(
                "contributors");
        contributorsList = Arrays.asList(contributorsArray);
        assertTrue(contributorsList.contains("Jacky"));
        assertEquals("Administrator",
                childFile3.getProperty("dublincore", "creator"));
    }

}
