/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.impl.PermissionFilter;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestPermissionFilter {

    @Inject
    protected RepositorySettings settings;

    @Inject
    CoreSession session;

    @Before
    public void initializePermissions() throws ClientException {
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
    public void testIncludedPermissions() throws ClientException {
        try (CoreSession newSession = openSessionAs("foo")) {
            DocumentModel doc = newSession.createDocumentModel("/", "file",
                    "File");
            doc = newSession.createDocument(doc);
            assertNotNull(doc);

            PermissionFilter filter = new PermissionFilter(Arrays.asList(READ),
                    null);
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
    public void testExcludedPermissions() throws ClientException {
        try (CoreSession newSession = openSessionAs("foo")) {
            DocumentModel doc = newSession.createDocumentModel("/", "file",
                    "File");
            doc = newSession.createDocument(doc);
            assertNotNull(doc);

            PermissionFilter filter = new PermissionFilter(null,
                    Arrays.asList(READ));
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
    public void testIncludedAndExcludedPermissions() throws ClientException {
        try (CoreSession newSession = openSessionAs("foo")) {
            DocumentModel doc = newSession.createDocumentModel("/", "file",
                    "File");
            doc = newSession.createDocument(doc);
            assertNotNull(doc);

            PermissionFilter filter = new PermissionFilter(Arrays.asList(READ),
                    Arrays.asList(WRITE));
            assertTrue(filter.accept(doc));

            filter = new PermissionFilter(Arrays.asList(READ, "Foo"),
                    Arrays.asList(WRITE));
            assertFalse(filter.accept(doc));

            filter = new PermissionFilter(Arrays.asList(READ, "Foo"),
                    Arrays.asList(WRITE, "Bar"));
            assertFalse(filter.accept(doc));

            filter = new PermissionFilter(Arrays.asList(READ), Arrays.asList(
                    WRITE, "Bar"));
            assertTrue(filter.accept(doc));

            filter = new PermissionFilter(Arrays.asList(WRITE),
                    Arrays.asList("Bar"));
            assertFalse(filter.accept(doc));
        }
    }

    protected CoreSession openSessionAs(String username) throws ClientException {
        return settings.openSessionAs(username);
    }

}
