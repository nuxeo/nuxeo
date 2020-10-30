/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     tmartins
 */
package org.nuxeo.ecm.platform.usermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @author Thierry Martins
 */
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class TestUserAdapter extends UserManagerTestCase {

    @Test
    public void testAdministratorModel() throws Exception {
        DocumentModel userModel = userManager.getUserModel("Administrator");
        UserAdapter userAdapter = userModel.getAdapter(UserAdapter.class);

        List<String> groups = new ArrayList<>();
        groups.add("administrators");

        assertEquals("Administrator", userAdapter.getName());
        assertEquals("devnull@nuxeo.com", userAdapter.getEmail());
        assertEquals("user", userAdapter.getSchemaName());
        assertEquals(groups, userAdapter.getGroups());
        assertTrue(StringUtils.isEmpty(userAdapter.getFirstName()));
        assertTrue(StringUtils.isEmpty(userAdapter.getLastName()));
        assertTrue(StringUtils.isEmpty(userAdapter.getCompany()));
    }

}
