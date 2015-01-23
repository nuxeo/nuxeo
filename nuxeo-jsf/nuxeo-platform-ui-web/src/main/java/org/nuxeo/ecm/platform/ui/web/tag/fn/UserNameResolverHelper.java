/*
 * (C) Copyright 2007-20015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 *
 */

package org.nuxeo.ecm.platform.ui.web.tag.fn;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DataModel;
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
            try {
                displayName = (String) cache.get(login);
            } catch (IOException e) {
                log.error("Unable to access cache " + USERNAMECACHE + " for entry " + login, e);
            }
        }

        if (displayName == null) {
            displayName = computeUserFullName(login);
            if (cache != null) {
                try {
                    cache.put(login, displayName);
                } catch (IOException e) {
                    log.error("Unable to access cache " + USERNAMECACHE + " for entry " + login, e);
                }
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
        Session dirSession = Framework.getService(DirectoryService.class).open(dname);

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
            DataModel model = entry.getDataModel(um.getUserSchemaName());
            return computeUserFullName(model);
        }
    }

    protected String computeUserFullName(DataModel model) {
        String first = (String) model.getData(UserConfig.DEFAULT.firstNameKey);
        String last = (String) model.getData(UserConfig.DEFAULT.lastNameKey);
        String username = (String) model.getData(UserConfig.DEFAULT.nameKey);
        return userDisplayName(username, first, last);
    }

    protected String computeUserFullName(NuxeoPrincipal principal) {
        String first = principal.getFirstName();
        String last = principal.getLastName();
        return userDisplayName(principal.getName(), first, last);
    }

    protected String userDisplayName(String id, String first, String last) {
        if (first == null || first.length() == 0) {
            if (last == null || last.length() == 0) {
                return id;
            } else {
                return last;
            }
        } else {
            if (last == null || last.length() == 0) {
                return first;
            } else {
                return first + ' ' + last;
            }
        }
    }

    @Override
    public boolean aboutToHandleEvent(Event arg0) {
        return true;
    }

    @Override
    public void handleEvent(Event event) {
        if ("user_changed".equals(event.getId())) {
            String userName = (String) event.getData();
            Cache cache = getCache();
            if (cache != null) {
                try {
                    cache.invalidate(userName);
                } catch (IOException e) {
                    log.error("Unable to invalidate entry", e);
                }
            }
        }
    }

}
