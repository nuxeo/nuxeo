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

package org.nuxeo.ecm.core.permissions;


import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 *
 * @author Razvan Caraghin
 *
 */
// TODO: rewrite these tests
public class TestPermissions extends RepositoryTestCase {

    Session session;
//
//    IWorkspace workspaceBean;
//
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // creating the session
//        Session session = RepositoryTestCase.getDemoRepository().getSession(new SessionContextImpl());
//        workspace = session.getWorkspace();
//
//        // set a custom ACP on the workspace
//        ACP acp = null;
//
//        acp = new ACPImpl();
//        acp.setOwners(Arrays.asList(new String[] { "q", "useradmin" }));
//
//        ACEImpl ace1 = new ACEImpl(ACEType.GRANT, Arrays.asList(new String[] {
//                "q", "useradmin" }), Arrays.asList(new String[] { "Read",
//                "Write", "Remove" }));
//        ACEImpl ace2 = new ACEImpl(ACEType.GRANT, Arrays
//                .asList(new String[] { "user" }), Arrays
//                .asList(new String[] { "Read" }));
//
//        ACLImpl acl = new ACLImpl("acl1", Arrays
//                .asList(new ACE[] { ace1, ace2 }));
//        acp.addACL(acl);
//        workspace.setACP(acp);
//
//        workspace.save();
    }
//
//        // XXX Standard JAAS login
//        // LoginContext lc = null;
//        // try {
//        //
//        // NXUserDTO userDTO = new NXUserDTO();
//        // userDTO.setUsername("useradmin");
//        // userDTO.setPassword("useradmin");
//        // lc = new LoginContext("nuxeo.ecm", userDTO);
//        //
//        // lc.login();
//        //
//        // InitialContext ctx;
//        // List<IItem> list = null;
//        //
//        // ctx = new InitialContext();
//        // workspaceBean = null;
//        //
//        // for (DocumentIterator iterator = workspace.getChildren(); iterator
//        // .hasNext();) {
//        // Document document = iterator.next();
//        //
//        // workspaceBean = (IWorkspace) ctx
//        // .lookup(JNDILocations.worskpaceLocation);
//        // workspaceBean.setCore(document);
//        //
//        // break;
//        // }
//        //
//        // } catch (LoginException le) {
//        // System.err
//        // .println("Cannot create LoginContext. " + le.getMessage());
//        //            System.exit(-1);
//        //        } catch (SecurityException se) {
//        //            System.err
//        //                    .println("Cannot create LoginContext. " + se.getMessage());
//        //            System.exit(-1);
//        //        }
//
//    } // TODO: test with multiple acls - test order?
//
    public void testACP() {
//
//        List<IItem> items = workspaceBean.getItems();
//
//        // Collection<ACL> acls = acp.getACLs();
//        // Assert.assertEquals(2, acls.size());
//        // Iterator<ACL> it = acls.iterator();
//        // acl = (ACLImpl) it.next();
//        // Assert.assertEquals("acl1", acl.getName());
//        // Collection<ACE> aces = acl.getACEs();
//        // Assert.assertEquals(2, aces.size());
//        // Assert.assertEquals(ACEType.GRANT, acl.getACEs().iterator().next()
//        // .getType());
//        // acl = (ACLImpl) it.next();
//        // Assert.assertEquals("acl2", acl.getName());
//        // Iterator<ACE> it2 = acl.getACEs().iterator();
//        // it2.next(); // skip first item
//        // Assert.assertEquals(ACEType.DENY, it2.next().getType());
//        //
//        // Assert.assertEquals(2, acp.getOwners().size());
//        // Assert.assertEquals("owner1", acp.getOwners().iterator().next());
//        // acp.setOwners(Arrays.asList(new String[] { "owner" }));
//        // workspace.setACP(acp);
//        // acp = workspace.getACP();
//        // Assert.assertEquals(1, acp.getOwners().size());
//        // Assert.assertEquals("owner", acp.getOwners().iterator().next());
//        //
//        // workspace.setACP(null);
//        // Assert.assertNull(workspace.getACP());
//
    }
//
}
