/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.usermanager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.AdministratorGroupsProvider;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager.MatchType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.Authenticator;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.event.EventService;

public class UserService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(UserService.class.getName());

    private static final Log log = LogFactory.getLog(UserService.class);

    private final List<UserManagerDescriptor> descriptors = new LinkedList<>();

    private UserManager userManager;

    public UserManager getUserManager() {
        if (userManager == null) {
            recomputeUserManager(false);
            EventService eventService = Framework.getService(EventService.class);
            eventService.addListener(UserManagerImpl.USERMANAGER_TOPIC, userManager);
        }
        return userManager;
    }

    protected void recomputeUserManager(boolean lazy) {
        if (lazy && userManager == null) {
            return;
        }
        UserManagerDescriptor merged = new UserManagerDescriptor();
        merged.userListingMode = "search_only";
        // BBB backward compatibility defaults
        merged.userDirectoryName = "userDirectory";
        merged.userEmailField = "email";

        merged.userSearchFields = new HashMap<>();
        merged.userSearchFields.put("username", MatchType.SUBSTRING);
        merged.userSearchFields.put("firstName", MatchType.SUBSTRING);
        merged.userSearchFields.put("lastName", MatchType.SUBSTRING);

        merged.groupDirectoryName = "groupDirectory";
        merged.groupLabelField = "grouplabel";
        merged.groupMembersField = "members";
        merged.groupSubGroupsField = "subGroups";
        merged.groupParentGroupsField = "parentGroups";

        merged.groupSearchFields = new HashMap<>();
        merged.groupSearchFields.put("groupname", MatchType.SUBSTRING);
        merged.groupSearchFields.put("grouplabel", MatchType.SUBSTRING);

        for (UserManagerDescriptor descriptor : descriptors) {
            merged.merge(descriptor);
        }
        Class<?> klass = merged.userManagerClass;
        if (userManager == null) {
            if (descriptors.isEmpty()) {
                throw new NuxeoException("No contributions registered for the userManager");
            }
            if (klass == null) {
                throw new NuxeoException("No class specified for the userManager");
            }
        }
        if (klass != null) {
            try {
                userManager = (UserManager) klass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException(e);
            }
        }
        userManager.setConfiguration(merged);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (Authenticator.class == adapter || UserManager.class == adapter
                || AdministratorGroupsProvider.class == adapter) {
            return adapter.cast(getUserManager());
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) {
        log.info("UserService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.info("UserService deactivated");
        if (userManager != null) {
            EventService eventService = Framework.getService(EventService.class);
            if (eventService != null) {
                eventService.removeListener(UserManagerImpl.USERMANAGER_TOPIC, userManager);
            }
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        descriptors.add((UserManagerDescriptor) contribution);
        recomputeUserManager(true);
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        descriptors.remove(contribution);
        if (Framework.getService(DirectoryService.class) != null) {
            try {
                recomputeUserManager(true);
            } catch (DirectoryException e) {
                log.debug(e); // at shutdown we may not have a userDirectory anymore
            }
        }
    }

}
