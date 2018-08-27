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
 *     btatar
 *     Damien METZLER (damien.metzler@leroymerlin.fr)
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.userworkspace.core.service;

import java.util.Deque;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionLocationService;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * This component is used to register the service that provide the userworkspace service support.
 *
 * @author btatar
 * @author Damien METZLER (damien.metzler@leroymerlin.fr)
 */
public class UserWorkspaceServiceImplComponent extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.platform.userworkspace.UserWorkspaceService";

    private static final Log log = LogFactory.getLog(UserWorkspaceService.class);

    protected Deque<UserWorkspaceDescriptor> descriptors = new LinkedList<>();

    protected UserWorkspaceService userWorkspaceService;

    @Override
    public void activate(ComponentContext context) {
        log.info("UserWorkspaceService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.info("UserWorkspaceService deactivated");
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == UserWorkspaceService.class) {
            return adapter.cast(getUserWorkspaceService());
        }
        if (adapter == CollectionLocationService.class) {
            return adapter.cast(getUserWorkspaceService());
        }
        return null;
    }

    private UserWorkspaceService getUserWorkspaceService() {
        if (userWorkspaceService == null) {
            Class<?> klass = getConfiguration().getUserWorkspaceClass();
            if (klass == null) {
                throw new NuxeoException("No class specified for the userWorkspace");
            }
            try {
                userWorkspaceService = (UserWorkspaceService) klass.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Failed to instantiate class " + klass, e);
            }
        }
        return userWorkspaceService;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        descriptors.add((UserWorkspaceDescriptor) contribution);
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        descriptors.remove(contribution);
    }

    protected void recompute() {
        userWorkspaceService = null;
    }

    public String getTargetDomainName() {
        return getConfiguration().getTargetDomainName();
    }

    public UserWorkspaceDescriptor getConfiguration() {
        return descriptors.getLast();
    }

    // for tests only
    public static void reset() {
        UserWorkspaceServiceImplComponent s = (UserWorkspaceServiceImplComponent) Framework.getRuntime().getComponent(
                NAME);
        s.userWorkspaceService = null;
    }

}
