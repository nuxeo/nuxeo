/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.user.center.profile.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.userpreferences.UserPreferencesService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.user.center.profile.UserProfile;
import org.nuxeo.ecm.user.center.profile.UserProfileConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests for {@link UserProfile}
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.4.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy( { "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.localconfiguration.simple",
        "org.nuxeo.ecm.platform.userpreferences",
        "org.nuxeo.ecm.user.center.profile" })
public class TestUserProfile {

    @Inject
    CoreSession session;

    @Inject
    UserPreferencesService userPreferencesService;

    @Inject
    UserWorkspaceService userWorkspaceService;

    @Test
    public void testGetUserProfile() throws Exception {
        DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(session, null);
        userWorkspace.addFacet(UserProfileConstants.USER_PROFILE_FACET);
        userWorkspace.setPropertyValue(UserProfileConstants.USER_PROFILE_PHONENUMBER_FIELD, "555-1234");
        userWorkspace = session.saveDocument(userWorkspace);
        session.save();
        UserProfile userProfile = userPreferencesService.getUserPreferences(session, UserProfile.class, UserProfileConstants.USER_PROFILE_FACET);
        assertNotNull(userProfile);
        assertEquals("555-1234", userProfile.getPhoneNumber());
    }

}
