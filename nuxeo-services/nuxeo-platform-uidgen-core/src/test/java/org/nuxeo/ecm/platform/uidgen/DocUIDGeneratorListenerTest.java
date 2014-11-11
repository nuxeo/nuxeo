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

import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Simple test Case for DocUIDGeneratorListener
 *
 * @author Julien Thimonier <jt@nuxeo.com>
 */
public class DocUIDGeneratorListenerTest extends UIDGeneratorTestCase {

    protected DocumentModel createFileDocument() throws ClientException {

        DocumentModel fileDoc = session.createDocumentModel("/",
                "testFile", "Note");

        fileDoc.setProperty("dublincore", "title", "TestFile");
        fileDoc.setProperty("dublincore", "description", "RAS");

        fileDoc = session.createDocument(fileDoc);

        session.saveDocument(fileDoc);
        session.save();

        return fileDoc;
    }

    @Test
    public void testListener() throws ClientException {
        DocumentModel doc = createFileDocument();
        assertNotNull(doc.getProperty("uid", "uid"));
    }

}
