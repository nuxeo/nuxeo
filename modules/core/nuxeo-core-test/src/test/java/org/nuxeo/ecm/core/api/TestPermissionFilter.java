/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.PermissionFilter;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestPermissionFilter {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    CoreSession session;

    @Before
    public void initializePermissions() {
        DocumentModel root = session.getRootDocument();
        ACP acp = root.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("foo", READ, true));
        acl.add(new ACE("foo", ADD_CHILDREN, true));
        acp.addACL(acl);
        root.setACP(acp, true);
        session.save();
    }

    @Test
    public void testIncludedPermissions() {
        try (CloseableCoreSession newSession = coreFeature.openCoreSession("foo")) {
            DocumentModel doc = newSession.createDocumentModel("/", "file", "File");
            doc = newSession.createDocument(doc);
            assertNotNull(doc);

            PermissionFilter filter = new PermissionFilter(Arrays.asList(READ), null);
            assertTrue(filter.accept(doc));

            filter = new PermissionFilter(Arrays.asList(WRITE), null);
            assertFalse(filter.accept(doc));

            filter = new PermissionFilter("Bar", true);
            assertFalse(filter.accept(doc));

            filter = new PermissionFilter(READ, true);
            assertTrue(filter.accept(doc));
        }
    }

    @Test
    public void testExcludedPermissions() {
        try (CloseableCoreSession newSession = coreFeature.openCoreSession("foo")) {
            DocumentModel doc = newSession.createDocumentModel("/", "file", "File");
            doc = newSession.createDocument(doc);
            assertNotNull(doc);

            PermissionFilter filter = new PermissionFilter(null, Arrays.asList(READ));
            assertFalse(filter.accept(doc));

            filter = new PermissionFilter(null, Arrays.asList("Foo"));
            assertTrue(filter.accept(doc));

            filter = new PermissionFilter("Bar", false);
            assertTrue(filter.accept(doc));

            filter = new PermissionFilter(READ, false);
            assertFalse(filter.accept(doc));
        }
    }

    @Test
    public void testIncludedAndExcludedPermissions() {
        try (CloseableCoreSession newSession = coreFeature.openCoreSession("foo")) {
            DocumentModel doc = newSession.createDocumentModel("/", "file", "File");
            doc = newSession.createDocument(doc);
            assertNotNull(doc);

            PermissionFilter filter = new PermissionFilter(Arrays.asList(READ), Arrays.asList(WRITE));
            assertTrue(filter.accept(doc));

            filter = new PermissionFilter(Arrays.asList(READ, "Foo"), Arrays.asList(WRITE));
            assertFalse(filter.accept(doc));

            filter = new PermissionFilter(Arrays.asList(READ, "Foo"), Arrays.asList(WRITE, "Bar"));
            assertFalse(filter.accept(doc));

            filter = new PermissionFilter(Arrays.asList(READ), Arrays.asList(WRITE, "Bar"));
            assertTrue(filter.accept(doc));

            filter = new PermissionFilter(Arrays.asList(WRITE), Arrays.asList("Bar"));
            assertFalse(filter.accept(doc));
        }
    }

}
