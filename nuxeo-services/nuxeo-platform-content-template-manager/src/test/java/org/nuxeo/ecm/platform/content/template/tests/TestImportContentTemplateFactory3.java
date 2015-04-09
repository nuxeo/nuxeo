/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.content.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@LocalDeploy("org.nuxeo.ecm.platform.content.template.tests:OSGI-INF/test-import-data3-content-template-contrib.xml")
public class TestImportContentTemplateFactory3 extends ImportContentTemplateFactoryTestCase {

    @Test
    public void testData3ImportFactory() throws Exception {
        service.executeFactoryForType(session.getRootDocument());

        DocumentModel helloDoc = session.getDocument(new PathRef("/default-domain/workspaces/workspace/hello.pdf"));
        assertNotNull(helloDoc);
        assertEquals(helloDoc.getType(), "File");
    }

}
