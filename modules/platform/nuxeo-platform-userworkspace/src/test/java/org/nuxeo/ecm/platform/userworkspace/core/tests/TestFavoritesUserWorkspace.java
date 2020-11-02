/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.userworkspace.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.FavoritesManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.platform.collections.core")
@Deploy("org.nuxeo.ecm.platform.web.common")
public class TestFavoritesUserWorkspace {

    @Inject
    protected CoreSession session;

    @Inject
    protected FavoritesManager favoritesManager;

    @Test
    public void testFavoritesWithoutDomain() {
        DocumentModel testWorkspace = session.createDocumentModel("/default-domain/workspaces", "testWorkspace",
                "Workspace");
        testWorkspace = session.createDocument(testWorkspace);
        DocumentModel testFile = session.createDocumentModel(testWorkspace.getPathAsString(), "foo", "File");
        testFile = session.createDocument(testFile);
        favoritesManager.addToFavorites(testFile, session);
        assertTrue(favoritesManager.isFavorite(testFile, session));

        // remove the only domain
        session.removeDocument(new PathRef("/default-domain"));

        // no user favorites, always false
        assertFalse(favoritesManager.isFavorite(testFile, session));
        assertFalse(favoritesManager.isFavorite(testWorkspace, session));

        try {
            favoritesManager.addToFavorites(testFile, session);
            fail("Should have raised DocumentNotFoundException");
        } catch (DocumentNotFoundException e) {
            assertEquals("No user favorites found", e.getMessage());
        }

        try {
            favoritesManager.removeFromFavorites(testFile, session);
            fail("Should have raised DocumentNotFoundException");
        } catch (DocumentNotFoundException e) {
            assertEquals("No user favorites found", e.getMessage());
        }
    }
}
