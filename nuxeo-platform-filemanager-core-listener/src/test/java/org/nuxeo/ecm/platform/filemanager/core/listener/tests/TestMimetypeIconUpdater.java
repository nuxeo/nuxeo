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

public class TestMimetypeIconUpdater extends AbstractListenerTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.api");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.filemanager.core.listener");

        deployContrib("org.nuxeo.ecm.platform.filemanager.core.listener",
                "OSGI-INF/nxfilemanager-core-listener.xml");
    }

    public void testMimeTypeUpdater() throws Exception {
        DocumentModel file = createFileDocument(false);
        Blob blob = (Blob) file.getProperty("file", "content");
        assertNotNull(blob);
        String mt = blob.getMimeType();
        assertNotNull(mt);
        assertEquals("application/pdf", mt);

        String icon = (String)file.getProperty("common", "icon");
        assertNotNull(icon);
        assertEquals("/icons/pdf.png", icon);
    }
}
