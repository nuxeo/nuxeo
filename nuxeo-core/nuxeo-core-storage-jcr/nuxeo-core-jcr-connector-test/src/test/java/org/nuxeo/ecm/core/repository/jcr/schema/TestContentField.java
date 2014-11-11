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

package org.nuxeo.ecm.core.repository.jcr.schema;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

public class TestContentField extends RepositoryTestCase {

    private Session session;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // start the repository -> types will be automatically imported
        // on the first start of the repo
        session = getRepository().getSession(null);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        session = null;
    }


    public void testBlobProperties() throws Exception {
        Document doc = session.getRootDocument().addChild("doc1", "File");
        Blob cs = new StringBlob("the content", "text/plain", "UTF-8");
        cs.setDigest("xxxxxxx");
        cs.setFilename("the name");

        doc.setContent("content", cs);


        Blob cs2 = doc.getContent("content");
        assertEquals(cs.getEncoding(), cs2.getEncoding());
        assertEquals(cs.getMimeType(), cs2.getMimeType());
        assertEquals(cs.getString(), cs2.getString());
        assertEquals(cs.getFilename(), cs2.getFilename());
        assertEquals(cs.getDigest(), cs2.getDigest());

        assertEquals("the name", cs.getFilename());
        assertEquals("xxxxxxx", cs.getDigest());
        assertEquals("the content", cs.getString());
        assertEquals("UTF-8", cs.getEncoding());
        assertEquals("text/plain", cs.getMimeType());
    }

}
