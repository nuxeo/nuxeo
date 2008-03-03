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
 * $Id: TestJCRLifeCycleManager.java 19318 2007-05-24 18:48:39Z fguillaume $
 */

package org.nuxeo.ecm.core.repository.jcr.lifecycle;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.JCRLifeCycleManager;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 *
 * Test the JCRLifeCycleManager which deals with the storage abstraction.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestJCRLifeCycleManager extends RepositoryTestCase {

    Session session;
    Document root;

    private JCRLifeCycleManager manager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deploy("LifeCycleService.xml");
        deploy("LifeCycleManagerTestExtensions.xml");
        session = getRepository().getSession(null);
        root = session.getRootDocument();
        manager = new JCRLifeCycleManager();
    }

    @Override
    protected void tearDown() throws Exception {
        manager = null;
        session.cancel();
        super.tearDown();
    }

    public void testSetGetState() throws LifeCycleException, DocumentException {
        Document doc = root.addChild("doc_lifecycle", "File");
        root.save();
        manager.setState(doc, "pending");
        doc.save();
        assertEquals("pending", manager.getState(doc));

        manager.setState(doc, "validated");
        doc.save();
        assertEquals("validated", manager.getState(doc));
    }

    public void testSetGetLifeCyclePolicy() throws LifeCycleException, DocumentException {
        Document doc = root.addChild("doc_lifecycle2", "File");
        root.save();
        manager.setPolicy(doc, "default");
        doc.save();
        assertEquals("default", manager.getPolicy(doc));
    }

}
