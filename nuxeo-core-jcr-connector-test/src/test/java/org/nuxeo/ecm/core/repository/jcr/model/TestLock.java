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

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * TODO: refactor this test
 */
public class TestLock extends RepositoryTestCase {

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
    public void testLock() throws Exception {
        Document doc = root.addChild("doc", "File");

        String lockKey = doc.getLock();
        assertNull(lockKey);
        doc.setLock("bstefanescu");

        lockKey = doc.getLock();
        assertEquals("bstefanescu", lockKey);

        lockKey = doc.unlock();
        assertEquals("bstefanescu", lockKey);

        lockKey = doc.getLock();
        assertNull(lockKey);
    }

}
