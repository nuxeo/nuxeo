/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.nuxeo.runtime.test.runner.Deploy;

@Deploy("org.nuxeo.ecm.platform.content.template.tests:OSGI-INF/test-import-data2-content-template-contrib.xml")
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
