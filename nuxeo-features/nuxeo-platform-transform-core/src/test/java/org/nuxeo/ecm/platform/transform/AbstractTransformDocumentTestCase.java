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
 * $Id: AbstractTransformDocumentTestCase.java 24475 2007-09-03 06:03:57Z janguenot $
 */
package org.nuxeo.ecm.platform.transform;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.common.utils.SerializableHelper;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public abstract class AbstractTransformDocumentTestCase extends NXRuntimeTestCase {

    protected TransformDocument document;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.transform.tests",
                "nxmimetype-service.xml");
    }

    public void testFileGeneration() throws Exception {
        InputStream stream = document.getBlob().getStream();
        assertTrue(stream.available() > 0);
    }

    public void testInstanceSerialization() throws IOException {
        assertTrue(SerializableHelper.isSerializable(document));
    }

}
