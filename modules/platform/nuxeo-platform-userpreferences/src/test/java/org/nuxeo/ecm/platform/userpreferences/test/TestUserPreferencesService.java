/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.platform.userpreferences.test;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.userpreferences.SimpleUserPreferences;
import org.nuxeo.ecm.platform.userpreferences.UserPreferencesService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests for {@link UserPreferencesService}
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.5
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.localconf")
@Deploy("org.nuxeo.ecm.directory.api")
@Deploy("org.nuxeo.ecm.platform.userpreferences")
public class TestUserPreferencesService {

    @Inject
    CoreSession session;

    @Inject
    UserPreferencesService userPreferencesService;

    @Inject
    UserWorkspaceService userWorkspaceService;

    @Test
    public void testGetSimpleUserPreferences() throws Exception {
        SimpleUserPreferences simpleUserPref = userPreferencesService.getSimpleUserPreferences(session);
        DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(session, null);
        assertEquals(userWorkspace.getRef(), simpleUserPref.getDocumentRef());
        simpleUserPref.put("foo", "bar");
        simpleUserPref.save(session);
        simpleUserPref = userPreferencesService.getSimpleUserPreferences(session);
        assertEquals("bar", simpleUserPref.get("foo"));
    }

}
