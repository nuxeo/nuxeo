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

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Warning: this is more integration test than unit tests.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestTypeMapping extends RepositoryTestCase {

    Document doc;
    Session session;

    @Override
    protected void setUp() throws Exception {
        // do nothing
    }

    @Override
    protected void tearDown() throws Exception {
        // do nothing
    }

    protected void start() throws Exception {
        super.setUp();
    }

    protected void shutdown() throws Exception {
        super.tearDown();
    }

    public void testDummy() {
        //
    }

    // TODO: not yet impl
    public void __testReimport() throws Exception {
        start();

        try {

            session = getRepository().getSession(null);
            doc = session.getRootDocument().addChild("mydoc", "File");

            try {
                doc.setLock("bstefanescu");
                fail("node type should not exists");
            } catch (DocumentException e) {
                // do nothing
            }

//        ArrayList<NodeTypeDef> ntDefs = new ArrayList<NodeTypeDef>();
//        ntDefs.add(BuiltinTypes.createSystemSchemaNodeType());
//        BuiltinTypes.reregisterNodeTypes((JCRSession)session, ntDefs);
//
//        doc = session.getRootDocument().getChild("mydoc");
//        String lockKey = doc.getLock();
//        assertNull(lockKey);
//
//        doc.setLock("bstefanescu");
//        lockKey = doc.getLock();
//        assertEquals("bstefanescu", lockKey);

        } finally {
            shutdown();
        }
    }

}
