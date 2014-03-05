/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.user.center.profile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ UserProfileFeature.class })
@RepositoryConfig
@LocalDeploy({ "org.nuxeo.ecm.user.center.profile:OSGI-INF/test-sql-directories-contrib.xml",
        "org.nuxeo.ecm.user.center.profile:OSGI-INF/test-core-types-contrib.xml",
        "org.nuxeo.ecm.user.center.profile:OSGI-INF/user-profile-test-not-ok-contrib.xml" })
public class TestUserProfileImporterNotOk extends AbstractUserProfileImporterTest {

    @Test
    public void serviceRegistration() {
        assertNotNull(userProfileService);
    }

    @Test
    public void userProfileImportsShouldFail() throws Exception {

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
                    Assert.assertEquals(propertyName + "property list should be empty",
                            0, ((String[]) propertyValue).length);
                } else {
                    assertNull(propertyName + " property value should be null", propertyValue);
                }
            }
        }
    }
}
