/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.content.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 10.10-HF26
 */
public class TestSimpleTemplateBasedRoot extends ImportContentTemplateFactoryTestCase {

    /**
     * NXP-28958
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.content.template.tests:OSGI-INF/test-add-new-content-template.xml")
    public void testAddingNewTemplateContent() {
        DocumentModel documentModel = session.getDocument(new PathRef("/MyFolderTemplateName"));
        assertNotNull(documentModel);
        ACP acp = documentModel.getACP();
        assertNotNull(acp);
        ACL acl = acp.getOrCreateACL();
        assertNotNull(acl);
        assertEquals(2, acl.getACEs().length);
        List<String> users = Arrays.stream(acl.getACEs()).map(ACE::getUsername).collect(Collectors.toList());
        assertTrue(users.contains("Administrator"));
        assertTrue(users.contains("administrators"));
    }

}
