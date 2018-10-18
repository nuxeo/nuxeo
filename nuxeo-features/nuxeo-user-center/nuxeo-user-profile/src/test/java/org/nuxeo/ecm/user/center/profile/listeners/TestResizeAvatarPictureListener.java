/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.user.center.profile.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_AVATAR_FIELD;
import static org.nuxeo.ecm.user.center.profile.listeners.ResizeAvatarPictureListener.RESIZED_IMAGE_HEIGHT;
import static org.nuxeo.ecm.user.center.profile.listeners.ResizeAvatarPictureListener.RESIZED_IMAGE_WIDTH;

import java.io.Serializable;
import java.net.URL;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.user.center.profile.UserProfileConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy("org.nuxeo.ecm.platform.userworkspace.types")
@Deploy("org.nuxeo.ecm.platform.userworkspace.api")
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.user.center.profile")
@Deploy("org.nuxeo.ecm.platform.web.common")
public class TestResizeAvatarPictureListener {

    @Inject
    CoreSession session;

    @Inject
    UserWorkspaceService userWorkspaceService;

    @Inject
    MimetypeRegistry mimeypeRegistry;

    ResizeAvatarPictureListener underTest;

    @Test
    public void testResizeAvatar() {
        DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(session);
        userWorkspace.addFacet(UserProfileConstants.USER_PROFILE_FACET);

        ImagingService imagingService = Framework.getService(ImagingService.class);
        assertNotNull(imagingService);

        underTest = new ResizeAvatarPictureListener();

        Blob tooBigAvatar = lookForAvatarBlob("data/BigAvatar.jpg");
        assertNotNull(tooBigAvatar);

        underTest.resizeAvatar(userWorkspace, tooBigAvatar);
        Blob resizedImage = (Blob) userWorkspace.getPropertyValue(USER_PROFILE_AVATAR_FIELD);

        assertNotNull(resizedImage);
        assertNotEquals(tooBigAvatar, resizedImage);

        ImageInfo imageInfo = imagingService.getImageInfo(resizedImage);
        assertTrue(imageInfo.getWidth() < RESIZED_IMAGE_WIDTH);
        assertEquals(imageInfo.getHeight(), RESIZED_IMAGE_HEIGHT);

        Blob limitSizeAvatar = lookForAvatarBlob("data/MediumAvatar.jpg");
        assertNotNull(tooBigAvatar);

        userWorkspace.setPropertyValue(USER_PROFILE_AVATAR_FIELD, (Serializable) limitSizeAvatar);

        underTest.resizeAvatar(userWorkspace, limitSizeAvatar);
        resizedImage = (Blob) userWorkspace.getPropertyValue(USER_PROFILE_AVATAR_FIELD);

        assertNotNull(resizedImage);

        imageInfo = imagingService.getImageInfo(resizedImage);

        assertEquals(imageInfo.getWidth(), RESIZED_IMAGE_WIDTH);
        assertEquals(imageInfo.getHeight(), RESIZED_IMAGE_HEIGHT);

        Blob underLimitSizeAvatar = lookForAvatarBlob("data/SmallAvatar.jpg");
        assertNotNull(tooBigAvatar);

        userWorkspace.setPropertyValue(USER_PROFILE_AVATAR_FIELD, (Serializable) underLimitSizeAvatar);

        underTest.resizeAvatar(userWorkspace, underLimitSizeAvatar);
        resizedImage = (Blob) userWorkspace.getPropertyValue(USER_PROFILE_AVATAR_FIELD);

        assertNotNull(resizedImage);

        imageInfo = imagingService.getImageInfo(resizedImage);
        assertTrue(imageInfo.getWidth() < RESIZED_IMAGE_WIDTH);
        assertTrue(imageInfo.getHeight() < RESIZED_IMAGE_HEIGHT);
    }

    protected Blob lookForAvatarBlob(String avatarImagePath) {
        URL avatarURL = this.getClass().getClassLoader().getResource(avatarImagePath);
        String mimeType = mimeypeRegistry.getMimetypeFromExtension(FilenameUtils.getExtension(avatarImagePath));
        return new URLBlob(avatarURL, mimeType);
    }

}
