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
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.model;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.security.SecurityManager;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * TODO: refactor this test
 */
public class TestACP extends RepositoryTestCase {

    Session session;
    Document root;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // creating the session
        session = getRepository().getSession(null);
        root = session.getRootDocument();
    }

    //TODO: test with multiple acls - test order?
    public void testACP() throws Exception {
        _testAcp(root);
        Document doc = root.addChild("doc", "File");
        _testAcp(doc);
        Document folder = root.addChild("folder", "Folder");
        _testAcp(folder);
        Document file2 = folder.addChild("folder2", "File");
        _testAcp(file2);
    }

    private void _testAcp(Document doc) throws Exception {
        SecurityManager sm = doc.getSession().getSecurityManager();
        ACP acp = sm.getACP(doc);
        if (doc != root) {
            assertNull(acp);
        }

        acp = new ACPImpl();
        acp.setOwners(new String[] {"owner1", "owner2"});

        ACE ace1 = new ACE("princ1", "perm1", true);
        ACE ace2 = new ACE("princ2", "perm2", false);
        //ACE ace3 = new ACE("nobody", "delete", Permission.DENY);

        ACLImpl acl = new ACLImpl("acl1", false);
        acl.add(ace1);
        acl.add(ace2);
        acp.addACL(acl);

        acl = new ACLImpl("acl2", true);
        acl.add(ace1);
        acl.add(ace2);
        acp.addACL(acl);

        sm.setACP(doc, acp, true);

        acp = sm.getACP(doc);
        assertNotNull(acp);

        ACL[] acls = acp.getACLs();
        assertEquals(2, acls.length);
        acl = (ACLImpl) acls[0];
        assertEquals("acl1", acl.getName());
        assertFalse(acl.isReadOnly());
        ACE[] aces = acl.getACEs();
        assertEquals(2, aces.length);
        assertTrue(aces[0].isGranted());
        acl = (ACLImpl) acls[1];
        assertEquals("acl2", acl.getName());
//        assertTrue(acl.isReadOnly());
        ACE[] aces2 = acl.getACEs();
        assertTrue(aces2[1].isDenied());

        assertEquals(2, acp.getOwners().length);
        assertEquals("owner1", acp.getOwners()[0]);
        acp.setOwners(new String[] {"owner"});
        sm.setACP(doc, acp, true);
        acp = sm.getACP(doc);
        assertEquals(1, acp.getOwners().length);
        assertEquals("owner", acp.getOwners()[0]);

        sm.setACP(doc, null, true);
        assertNull(sm.getACP(doc));
    }

}
