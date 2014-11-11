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

package org.nuxeo.ecm.core.repository.jcr.model;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.schema.TypeConstants;

/**
 * Unit test for Session.
 *
 * @author <a href="mailto:lgiura@nuxeo.com">Leonard Giura</a>
 */
public class TestSession extends RepositoryTestCase {

    private Session session;
    private Document root;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        assertNotNull(root);
    }

    @Override
    public void tearDown() throws Exception {
        session.close();
        session = null;
        root = null;
        super.tearDown();
    }

    public void testGetName() throws DocumentException  {
        assertNotNull(root.getName());
    }

    public void testSave() throws Exception {
        root.addChild("testDoc", TypeConstants.DOCUMENT);
        Document doc = root.getChild("testDoc");
        session.save();
        assertNotNull(doc);
        root.removeChild("testDoc");
    }

    public void testGetSession() {
        assertNotNull(root.getSession());
    }

    public void testGetXAResource() {
        assertNotNull(session.getXAResource());
    }

    public void testGetDocument() throws Exception {
        root.addChild("testDoc", TypeConstants.DOCUMENT);
        Document doc = root.getChild("testDoc");
        assertNotNull(doc);
        assertNotNull(root.resolvePath("/testDoc"));
        root.removeChild("testDoc");
    }

    public void testGetTypeManager() {
        assertNotNull(session.getTypeManager());
    }

    public void testClose() throws Exception {
        boolean close;
        root.addChild("testDoc", TypeConstants.DOCUMENT);
        session.close();
        try {
            // if we can get the doc, means that the workspace was not closed
            root.getChild("testDoc");
            close = false;
        } catch (Exception e) {
            close = true;
        }
        assertTrue(close);
        // reopen repository to not interfere with tearDown()
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        root.removeChild("testDoc");
    }

}
