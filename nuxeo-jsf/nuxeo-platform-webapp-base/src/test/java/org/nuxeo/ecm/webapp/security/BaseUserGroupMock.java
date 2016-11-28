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
 *     dmetzler
 */
package org.nuxeo.ecm.webapp.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * @author dmetzler
 */
public class BaseUserGroupMock {

    /**
     * Return a mocked principal wich is admin or not
     *
     * @param username
     * @param admin
     * @return
     */
    protected Principal getMockedUser(String username, boolean admin) {
        Principal user = mock(NuxeoPrincipalImpl.class);
        when(((NuxeoPrincipal) user).isAdministrator()).thenReturn(admin);
        when(((NuxeoPrincipal) user).getName()).thenReturn(username);

        return user;
    }

    /**
     * @param username
     * @param isAdmin
     * @param userManager
     * @return
     */
    protected Principal getMockedUser(String username, boolean isAdmin, UserManager userManager) {
        Principal user = getMockedUser(username, isAdmin);
        when(userManager.getPrincipal(username)).thenReturn((NuxeoPrincipal) user);
        return user;
    }

    /**
     * Return a mocked group
     *
     * @param groupName
     * @return
     * @throws Exception
     */
    protected DocumentModel mockGroup(String groupName) throws Exception {

        DocumentModel group = mock(DocumentModel.class);
        when(group.getId()).thenReturn(groupName);

        // Just for BaseSession#isReadOnly to work
        when(group.getContextData()).thenReturn(new ScopedMap());

        return group;
    }

    /**
     * @param string
     * @return
     */
    protected DocumentModel mockUserDM(String username) {
        DocumentModel user = mock(DocumentModel.class);
        when(user.getId()).thenReturn(username);

        // Just for BaseSession#isReadOnly to work
        when(user.getContextData()).thenReturn(new ScopedMap());

        return user;
    }

    protected UserManager mockUserManager() {
        UserManager um = mock(UserManager.class);

        when(um.areUsersReadOnly()).thenReturn(false);
        when(um.getAnonymousUserId()).thenReturn(null);
        when(um.areGroupsReadOnly()).thenReturn(false);
        when(um.getAdministratorsGroups()).thenReturn(singleGroup("administrators"));
        when(um.getGroupsInGroup("administrators")).thenReturn(singleGroup("subadmin"));
        when(um.getGroup("administrators")).thenReturn(new NuxeoGroupImpl("administrators"));
        when(um.getGroup("")).thenReturn(new NuxeoGroupImpl("default"));
        return um;
    }

    protected List<String> singleGroup(String groupName) {
        return Arrays.asList(new String[] { groupName });
    }

}
