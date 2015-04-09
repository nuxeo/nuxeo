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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@LocalDeploy("org.nuxeo.ecm.platform.content.template.tests:OSGI-INF/test-import-data2-content-template-contrib.xml")
public class TestImportContentTemplateFactory2 extends ImportContentTemplateFactoryTestCase {

    @Test
    public void testData2ImportFactory() throws Exception {
        service.executeFactoryForType(session.getRootDocument());

        DocumentModel testZipfolder = session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/testZipImport"));
        assertNotNull(testZipfolder);
        DocumentModelList childs = session.getChildren(testZipfolder.getRef());
        assertNotNull(childs);
        assertEquals(childs.size(), 3);
        DocumentModel subNote = session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/testZipImport/SubFolder/SubNote"));
        assertNotNull(subNote);
        assertEquals(subNote.getType(), "Note");
    }

}
