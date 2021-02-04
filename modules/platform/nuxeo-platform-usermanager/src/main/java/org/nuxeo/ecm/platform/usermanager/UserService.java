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

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.AdministratorGroupsProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.Authenticator;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.event.EventService;

public class UserService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(UserService.class.getName());

    protected static final String XP = "userManager";

    private UserManager userManager;

    @Override
    public void start(ComponentContext context) {
        UserManagerDescriptor merged = this.<UserManagerDescriptor> getRegistryContribution(
                XP).orElseThrow(() -> new NuxeoException("No contributions registered for the userManager"));

        Class<?> klass = merged.userManagerClass;
        if (klass == null) {
            throw new NuxeoException("No class specified for the userManager");
        }
        try {
            userManager = (UserManager) klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
        userManager.setConfiguration(merged);

        EventService eventService = Framework.getService(EventService.class);
        eventService.addListener(UserManagerImpl.USERMANAGER_TOPIC, userManager);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        if (userManager != null) {
            EventService eventService = Framework.getService(EventService.class);
            if (eventService != null) {
                eventService.removeListener(UserManagerImpl.USERMANAGER_TOPIC, userManager);
            }
        }
        userManager = null;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (Authenticator.class == adapter || UserManager.class == adapter
                || AdministratorGroupsProvider.class == adapter) {
            return adapter.cast(getUserManager());
        }
        return null;
    }

}
