/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.shell.commands.repository;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

/**
 * Testing local acls commands.
 *
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
public class TestLocalAceCommands extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    public void testAddAce() throws Exception {
        // create new document for testing
        DocumentModel doc = session.createDocumentModel("/",
                "docname_for_testing", "File");
        assertNotNull(doc);
        doc = session.createDocument(doc);
        session.save();
        closeSession();

        // making sure that User1 doesn't have read permission
        session = openSessionAs("User1");
        assertFalse("User1 should not have read permission",
                session.hasPermission(new PathRef("/docname_for_testing"),
                        SecurityConstants.READ));
        closeSession();

        // change permission giving everything
        openSession();
        AddLocalAceCommand addLocalAceCommand = new AddLocalAceCommand();
        addLocalAceCommand.addLocalAce(session, doc, "User1",
                SecurityConstants.EVERYTHING, true);
        session.save();
        closeSession();

        // making sure that user1 has read permission
        session = openSessionAs("User1");
        assertTrue("User1 should have read permission", session.hasPermission(
                new PathRef("/docname_for_testing"), SecurityConstants.READ));
        closeSession();

        // deny only write
        openSession();
        addLocalAceCommand.addLocalAce(session, doc, "User1",
                SecurityConstants.WRITE, false);
        session.save();
        closeSession();

        // making sure that user1 has read permission but not write
        session = openSessionAs("User1");
        assertTrue("User1 should have read permission", session.hasPermission(
                new PathRef("/docname_for_testing"), SecurityConstants.READ));
        assertFalse("User1 should not have write permission",
                session.hasPermission(new PathRef("/docname_for_testing"),
                        SecurityConstants.WRITE));
        closeSession();
    }

    public void testRemoveAce() throws Exception {
        // create new document for testing
        DocumentModel doc = session.createDocumentModel("/",
                "docname_for_testing", "File");
        assertNotNull(doc);
        doc = session.createDocument(doc);
        session.save();
        closeSession();

        // making sure that User1 doesn't have read permission
        session = openSessionAs("User1");
        assertFalse("User1 should not have read permission",
                session.hasPermission(new PathRef("/docname_for_testing"),
                        SecurityConstants.READ));
        closeSession();

        // change permission giving everything but write
        openSession();
        AddLocalAceCommand addLocalAceCommand = new AddLocalAceCommand();
        addLocalAceCommand.addLocalAce(session, doc, "User1",
                SecurityConstants.EVERYTHING, true);
        addLocalAceCommand.addLocalAce(session, doc, "User1",
                SecurityConstants.WRITE, false);
        session.save();
        closeSession();
        session = openSessionAs("User1");
        assertTrue("User1 should have read permission", session.hasPermission(
                new PathRef("/docname_for_testing"), SecurityConstants.READ));
        assertFalse("User1 should not have write permission",
                session.hasPermission(new PathRef("/docname_for_testing"),
                        SecurityConstants.WRITE));
        closeSession();

        // testing removing the write ace
        session = openSessionAs("User1");
        RmLocalAceCommand rmLocalAceCommand = new RmLocalAceCommand();
        rmLocalAceCommand.removeLocalAce(session, doc, 0);
        assertTrue("User1 should have read permission", session.hasPermission(
                new PathRef("/docname_for_testing"), SecurityConstants.READ));
        assertTrue("User1 should have write permission", session.hasPermission(
                new PathRef("/docname_for_testing"), SecurityConstants.WRITE));
    }

}
