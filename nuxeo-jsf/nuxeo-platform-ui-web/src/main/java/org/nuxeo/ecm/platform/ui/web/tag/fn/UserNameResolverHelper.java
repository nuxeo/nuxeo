/*
 * (C) Copyright 2007-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.tag.fn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Helper class to encapsulate userName => DisplayName resolution. Does direct access to the underlying directories to
 * avoid performance issues.
 *
 * @author tiry
 * @since 7.2
 */
public class UserNameResolverHelper implements EventListener {

    protected static final Log log = LogFactory.getLog(UserNameResolverHelper.class);

    public static final String USERNAMECACHE = "userDisplayName";

    protected volatile Cache cache;

    public String getUserFullName(String login) {

        String displayName = null;

        Cache cache = getCache();
        if (cache != null) {
            displayName = (String) cache.get(login);
        }

        if (displayName == null) {
            displayName = computeUserFullName(login);
            if (cache != null) {
                cache.put(login, displayName);
            }
        }

        if (displayName == null) {
            displayName = login;
        }
        return displayName;
    }

    protected Cache getCache() {
        Cache result = cache;
        if (result == null) {
            synchronized (this) {
                result = cache;
                if (result == null) {
                    CacheService cs = Framework.getService(CacheService.class);
                    if (cs != null) {
                        result = cs.getCache(USERNAMECACHE);
                        if (result != null) {
                            EventService es = Framework.getService(EventService.class);
                            es.addListener("usermanager", this);
                        }
                        cache = result;
                    }
                }
            }
        }
        return result;
    }

    protected String computeUserFullName(String login) {

        UserManager um = Framework.getService(UserManager.class);
        String dname = um.getUserDirectoryName();

        try (Session dirSession = Framework.getService(DirectoryService.class).open(dname)) {
            DocumentModel entry = dirSession.getEntry(login, false);
            if (entry == null) {
                // virtual user ?
                NuxeoPrincipal principal = um.getPrincipal(login);
                if (principal != null) {
                    return computeUserFullName(principal);
                } else {
                    return login;
                }
            } else {
                return computeUserFullName(entry, um.getUserSchemaName());
            }
        }
    }

    protected String computeUserFullName(DocumentModel entry, String schema) {
        String first = (String) entry.getProperty(schema, UserConfig.DEFAULT.firstNameKey);
        String last = (String) entry.getProperty(schema, UserConfig.DEFAULT.lastNameKey);
        String username = (String) entry.getProperty(schema, UserConfig.DEFAULT.nameKey);
        return Functions.userDisplayName(username, first, last);
    }

    protected String computeUserFullName(NuxeoPrincipal principal) {
        String first = principal.getFirstName();
        String last = principal.getLastName();
        return Functions.userDisplayName(principal.getName(), first, last);
    }

    @Override
    public void handleEvent(Event event) {
        if ("user_changed".equals(event.getId())) {
            String userName = (String) event.getData();
            Cache cache = getCache();
            if (cache != null) {
                cache.invalidate(userName);
            }
        }
    }

}
