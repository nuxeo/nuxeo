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

package org.nuxeo.ecm.platform.uidgen;

import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;

/**
 * Simple test Case for DocUIDGeneratorListener
 *
 * @author Julien Thimonier <jt@nuxeo.com>
 */
public class DocUIDGeneratorListenerTest extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        NamingContextFactory.setAsInitial();
        openRepository();
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.uidgen.core");
        deployContrib("org.nuxeo.ecm.platform.uidgen.core.tests",
                "nxuidgenerator-test-contrib.xml");
    }

    protected DocumentModel createFileDocument() throws ClientException {

        DocumentModel fileDoc = getCoreSession().createDocumentModel("/",
                "testFile", "Note");

        fileDoc.setProperty("dublincore", "title", "TestFile");
        fileDoc.setProperty("dublincore", "description", "RAS");

        fileDoc = getCoreSession().createDocument(fileDoc);

        getCoreSession().saveDocument(fileDoc);
        getCoreSession().save();

        return fileDoc;
    }

    public void testListener() throws ClientException {
        DocumentModel doc = createFileDocument();
        assertNotNull(doc.getProperty("uid", "uid"));
    }

}
