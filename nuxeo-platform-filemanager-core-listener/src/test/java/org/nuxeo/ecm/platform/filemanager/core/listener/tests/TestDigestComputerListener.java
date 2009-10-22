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

package org.nuxeo.ecm.platform.filemanager.core.listener.tests;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

public class TestDigestComputerListener extends AbstractListener {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployContrib("org.nuxeo.ecm.platform.filemanager.core.listener",
                "OSGI-INF/filemanager-digestcomputer-event-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.filemanager.core.listener.test",
                "OSGI-INF/nxfilemanager-digest-contrib.xml");
    }

    public void testDigest() throws Exception {
        DocumentModel file = createFileDocument(true);
        Blob blob = (Blob) file.getProperty("file", "content");
        assertNotNull(blob);

        String digest = blob.getDigest();
        assertNotNull(digest);
        assertFalse("".equals(digest));
        assertEquals("CJz5xUykO51gRRCIQadZ9dL20NPDd/O0yVBEgP13Skg=", digest);
    }

}
