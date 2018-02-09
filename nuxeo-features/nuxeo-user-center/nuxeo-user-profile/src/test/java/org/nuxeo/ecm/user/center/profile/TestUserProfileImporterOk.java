/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.user.center.profile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ UserProfileFeature.class, DirectoryFeature.class })
@RepositoryConfig
@Deploy({ "org.nuxeo.ecm.user.center.profile:OSGI-INF/test-sql-directories-contrib.xml",
        "org.nuxeo.ecm.user.center.profile:OSGI-INF/test-core-types-contrib.xml",
        "org.nuxeo.ecm.user.center.profile:OSGI-INF/user-profile-test-ok-contrib.xml" })
public class TestUserProfileImporterOk extends AbstractUserProfileImporterTest {

    @Test
    public void userProfileImportsShouldSucceed() throws Exception {
        // we use hot redeploy, which restarts the repository, so DBS Mem (which has no persistence) cannot be used
        assumeFalse("Cannot test on DBS Mem", coreFeature.getStorageConfiguration().isDBSMem());

        assertNotNull(userProfileService);

        Framework.getService(EventService.class).waitForAsyncCompletion();

        checkDocs();

        deployer.deploy("org.nuxeo.ecm.user.center.profile.test:OSGI-INF/user-profile-test-ok-no-update-contrib.xml");

        UserProfileImporter importer = new
                UserProfileImporter();
        importer.doImport(session);

        checkDocs();
    }

    private void checkDocs() throws Exception {

        File blobsFolder = getBlobsFolder();

        DocumentModel doc = userProfileService.getUserProfileDocument("user1", session);

        String uid = doc.getId();

        DataModel userProfileData = doc.getDataModel("userprofile");
        assertNotNull(userProfileData);

        Calendar birthDate = (Calendar) doc.getPropertyValue("userprofile:birthdate");
        assertNotNull(birthDate);
        assertEquals("01/01/2001",
                new SimpleDateFormat(ImporterConfig.DEFAULT_DATE_FORMAT).format(birthDate.getTime()));
        assertEquals("111-222-3333", doc.getPropertyValue("userprofile:phonenumber"));
        byte[] signatureBytes = FileUtils.readFileToByteArray(
                new File(blobsFolder.getAbsolutePath() + "/" + "signature1.jpg"));
        byte[] docSignatureBytes = ((Blob) doc.getPropertyValue("myuserprofile:signature")).getByteArray();
        assertArrayEquals(signatureBytes, docSignatureBytes);
        assertEquals(false, doc.getPropertyValue("myuserprofile:publicprofile"));
        List<String> skills = Arrays.asList((String[]) doc.getPropertyValue("myuserprofile:skills"));
        assertEquals(2, skills.size());
        assertTrue(skills.contains("reading"));
        assertTrue(skills.contains("writing"));

        doc = userProfileService.getUserProfileDocument("user2", session);
        birthDate = (Calendar) doc.getPropertyValue("userprofile:birthdate");
        assertEquals("02/02/2002",
                new SimpleDateFormat(ImporterConfig.DEFAULT_DATE_FORMAT).format(birthDate.getTime()));
        assertEquals("222-333-4444", doc.getPropertyValue("userprofile:phonenumber"));
        signatureBytes = FileUtils.readFileToByteArray(
                new File(blobsFolder.getAbsolutePath() + "/" + "signature2.jpg"));
        docSignatureBytes = ((Blob) doc.getPropertyValue("myuserprofile:signature")).getByteArray();
        assertArrayEquals(signatureBytes, docSignatureBytes);
        assertEquals(true, doc.getPropertyValue("myuserprofile:publicprofile"));
        skills = Arrays.asList((String[]) doc.getPropertyValue("myuserprofile:skills"));
        assertEquals(2, skills.size());
        assertTrue(skills.contains("reading"));
        assertTrue(skills.contains("arithmetic"));

        doc = userProfileService.getUserProfileDocument("user3", session);
        birthDate = (Calendar) doc.getPropertyValue("userprofile:birthdate");
        assertEquals("03/03/2003",
                new SimpleDateFormat(ImporterConfig.DEFAULT_DATE_FORMAT).format(birthDate.getTime()));
    }
}
