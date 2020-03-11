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

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.runtime.test.runner.Deploy;

@Deploy("org.nuxeo.ecm.platform.content.template.tests:OSGI-INF/test-import-data1-content-template-contrib.xml")
public class TestImportContentTemplateFactory1 extends ImportContentTemplateFactoryTestCase {

    @Test
    public void testData1ImportFactory() throws Exception {
        service.executeFactoryForType(session.getRootDocument());

        assertNotNull(session.getDocument(new PathRef("/default-domain/workspaces/workspace/test1")));
        assertNotNull(session.getDocument(new PathRef("/default-domain/workspaces/workspace/test1/testFile.txt")));
        assertNotNull(session.getDocument(new PathRef("/default-domain/workspaces/workspace/test1/testFile2.txt")));
        assertNotNull(session.getDocument(new PathRef("/default-domain/workspaces/workspace/test1/testFile3.txt")));
        assertNotNull(session.getDocument(new PathRef("/default-domain/workspaces/workspace/test1/nestedFolder/")));
        assertNotNull(session.getDocument(new PathRef(
                "/default-domain/workspaces/workspace/test1/nestedFolder/nestedFile.txt")));
    }

}
