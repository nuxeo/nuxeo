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
 */
package org.nuxeo.ecm.user.center.profile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
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
        "org.nuxeo.ecm.user.center.profile:OSGI-INF/user-profile-test-not-ok-contrib.xml" })
public class TestUserProfileImporterNotOk extends AbstractUserProfileImporterTest {

    @Test
    public void userProfileImportsShouldFail() throws Exception {

        assertNotNull(userProfileService);

        List<String> propertyNameList = Arrays.asList(new String[] {"userprofile:avatar","userprofile:birthdate",
                "userprofile:phonenumber","userprofile:gender","userprofile:locale",
                "myuserprofile:schoolworkinfo","myuserprofile:signature","myuserprofile:startdate",
                "myuserprofile:skills","myuserprofile:publicprofile"});

        for (int i = 1; i < 4; i++) {
            String username = "user" + i;
            DocumentModel doc = userProfileService.getUserProfileDocument(username, session);
            assertNotNull(doc);
            for (String propertyName : propertyNameList) {
                Property property = doc.getProperty(propertyName);
                Type propertyType = property.getField().getType();
                Object propertyValue = property.getValue();
                if (propertyType instanceof BooleanType) {
                    Assert.assertFalse(propertyName + " property value should be false",
                            (Boolean) propertyValue);
                } else if (propertyType.isListType()) {
                    Assert.assertNull(propertyName + "property list should be empty", propertyValue);
                } else {
                    assertNull(propertyName + " property value should be null", propertyValue);
                }
            }
        }
    }
}
