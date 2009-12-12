/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webdav;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Tests that the runtime starts correctly and we can play with the core.
 */
public class RuntimeTest extends Assert {

    @Before
    public void setUp() throws Exception {
        Server.startRuntime();
    }

    @Test
    public void testRuntimeStarts() throws Exception {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
        Repository repo = rm.getDefaultRepository();
        assertNotNull(repo);

        CoreSession session = repo.open();
        assertNotNull(session);

        DocumentModel doc = session.getDocument(new PathRef("/"));
        assertEquals("Root", doc.getType());
    }

    // Don't run for now.
    public void testModifyDoc() throws Exception {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
        Repository repo = rm.getDefaultRepository();
        assertNotNull(repo);

        CoreSession session = repo.open();
        assertNotNull(session);

        DocumentModel doc = session.getDocument(new PathRef("/"));
        assertEquals("Root", doc.getType());
        doc.setPropertyValue("dc:title", "test");
        session.saveDocument(doc);
        session.save();

        DocumentModel doc2 = session.getDocument(new PathRef("/"));
        String title = doc2.getTitle();
        assertEquals("test", title);
    }

}
