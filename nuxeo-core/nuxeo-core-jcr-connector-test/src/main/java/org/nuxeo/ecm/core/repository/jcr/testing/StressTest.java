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

package org.nuxeo.ecm.core.repository.jcr.testing;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.model.Session;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StressTest extends RepositoryTestCase {

    Session session;
    Document parent;
    Document doc;
    final int runNo;

    public StressTest(int runNo) {
        super("stressMe");
        this.runNo = runNo;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.BUNDLE,
                "CoreTestExtensions.xml");
        // creating the session
        session = getRepository().getSession(null);
        // adding a folder and a child doc
        parent = session.getRootDocument().addChild("child_folder_from_test", "Folder");
        doc = parent.addChild("child_document_from_test", "MyDocType");
        // adding complex property to a normal doc
        Property docProp = doc.getProperty("my:name");
        assertNotNull(docProp);
        session.save();
    }

    @Override
    protected void tearDown() throws Exception {
        parent.remove();
        session.close();
        doc = null;
        parent = null;
        session = null;
        super.tearDown();
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for org.nuxeo.ecm.core.repository.jcr.model");
        for (int i = 0; i < 100; i++) {
            final int k = i + 1;
                suite.addTest(new StressTest(k));
        }
        return suite;
    }

}
