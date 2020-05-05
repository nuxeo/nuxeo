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

package org.nuxeo.ecm.platform.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * The root init acl which happens at
 * {@link org.nuxeo.ecm.platform.content.template.factories.SimpleTemplateBasedRootFactory}. Should occur only once, at
 * the beginning when we add the first children of the root document.
 * <p>
 * Deploying a new contribution for a given test method, will call the {@code SimpleTemplateBasedRootFactory} for a
 * second time, but as the root documents already contains children, we should not init the acl of the root document.
 *
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestSimpleTemplateBasedRootAudit {

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pps;

    @Test
    @Deploy("org.nuxeo.ecm.platform.audit.tests:test-add-content-template-contrib.xml")
    public void shouldInitRootAclOnlyOnce() {
        DocumentModel root = session.getDocument(new PathRef("/"));
        @SuppressWarnings("unchecked")
        PageProvider<LogEntry> pp = (PageProvider<LogEntry>) pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null, 3l,
                0l, new HashMap<String, Serializable>(), root);
        assertEquals(1, pp.getResultsCount());
        LogEntry entry = pp.getCurrentEntry();
        assertEquals("documentSecurityUpdated", entry.getEventId());
        assertEquals("Root", entry.getDocType());
        assertEquals("/", entry.getDocPath());

        // Check the permission of the root document
        ACP acp = root.getACP();
        assertNotNull(acp);
        ACL acl = acp.getOrCreateACL();
        assertNotNull(acl);
        assertEquals(2, acl.getACEs().length);
        List<String> users = Arrays.stream(acl.getACEs()).map(ACE::getUsername).collect(Collectors.toList());
        assertTrue(users.contains("Administrator"));
        assertTrue(users.contains("members"));

        // Check the permission of the added document template
        DocumentModel documentModel = session.getDocument(new PathRef("/MyFolderTemplateName"));
        acp = documentModel.getACP();
        assertNotNull(acp);
        acl = acp.getOrCreateACL();
        assertNotNull(acl);
        assertEquals(1, acl.getACEs().length);
        users = Arrays.stream(acl.getACEs()).map(ACE::getUsername).collect(Collectors.toList());
        assertEquals("John", acl.getACEs()[0].getUsername());
    }

}
