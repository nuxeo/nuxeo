/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Christophe Capon
 *
 */

package org.nuxeo.ecm.usersettings.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.usersettings.UserSettingsConstants;
import org.nuxeo.ecm.usersettings.UserSettingsDescriptor;
import org.nuxeo.ecm.usersettings.UserSettingsProviderDescriptor;
import org.nuxeo.ecm.usersettings.UserSettingsService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * This component is used to register the service that provide the userworkspace
 * service support.
 * 
 * @author btatar
 * @author Damien METZLER (damien.metzler@leroymerlin.fr)
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 */
public class UserSettingsServiceImplComponent extends DefaultComponent {

    public static final String NAME = "com.nuxeo.vilogia.usersettings.UserSettingsServiceComponent";

    private static final Log log = LogFactory.getLog(UserSettingsService.class);

    private static UserSettingsDescriptor descriptor;

    private static UserSettingsService userSettingsService;

    @Override
    public void activate(ComponentContext context) {
        log.info("UserSettingsService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.info("UserSettingsService deactivated");
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == UserSettingsService.class) {
            try {
                return adapter.cast(getUserSettingsService());
            } catch (Exception e) {
                log.error("error fetching UserSettingsService: ", e);
            }
        }
        return null;
    }

    private UserSettingsService getUserSettingsService() throws ClientException {

        if (userSettingsService == null) {

            Class<?> klass = descriptor.getUserSettingsClass();

            if (klass == null) {
                throw new ClientException(
                        "No class specified for the userPreferences");
            }
            try {
                userSettingsService = (UserSettingsService) klass.newInstance();
            } catch (Exception e) {
                throw new ClientException("Failed to instantiate class "
                        + klass, e);
            }
        }
        return userSettingsService;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {

        if (UserSettingsConstants.BASE_EXTENSION_POINT.equals(extensionPoint)) {
            descriptor = (UserSettingsDescriptor) contribution;
            log.info(String.format(
                    "Registered %s extension point with %s class.",
                    extensionPoint, contribution.getClass().getName()));
        } else if (UserSettingsConstants.SETTINGS_PROVIDER_EXTENSION_POINT.equals(extensionPoint)) {
            registerUserSettingsProvider(contribution, extensionPoint,
                    contributor);
        } else {
            throw new ClientException(extensionPoint
                    + " is not a valid extension point.");
        }

    }

    private void registerUserSettingsProvider(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {
        if (contribution instanceof UserSettingsProviderDescriptor) {
            UserSettingsProviderDescriptor desc = (UserSettingsProviderDescriptor) contribution;
            UserSettingsService svc = getUserSettingsService();
            svc.registerProvider(desc);
            log.info(String.format(
                    "Registered %s extension point with %s name.",
                    extensionPoint, desc.getType()));
        }
    }

    private void unregisterPendingProviders() throws ClientException {
        getUserSettingsService().clearProviders();
    }

    private void unregisterUserSettingsProvider(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {
        if (contribution instanceof UserSettingsProviderDescriptor) {
            UserSettingsProviderDescriptor desc = (UserSettingsProviderDescriptor) contribution;
            try {
                UserSettingsService svc = getUserSettingsService();
                svc.unRegisterProvider(desc.getType());
                log.info(String.format("Unregistering settings provider %s",
                        desc.getType()));
            } catch (Exception e) {
                throw new ClientException(
                        "Failed to unregister UserSettingsService", e);
            }
        }

    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws ClientException {

        if (UserSettingsConstants.BASE_EXTENSION_POINT.equals(extensionPoint)) {
            unregisterPendingProviders();
            try {
            } catch (Exception e) {
                log.error("Error when terminating service", e);
            }
            descriptor = null;
        } else if (UserSettingsConstants.SETTINGS_PROVIDER_EXTENSION_POINT.equals(extensionPoint)) {
            unregisterUserSettingsProvider(contribution, extensionPoint,
                    contributor);
        } else {
            throw new ClientException(extensionPoint
                    + " is not a valid extension point.");
        }

    }

    public static void reset() {
        userSettingsService = null;
    }

    public static Class<? extends UserSettingsService> getUserSettingsClass() {
        return descriptor.getUserSettingsClass();
    }

}