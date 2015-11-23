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

package com.nuxeo.ecm.usersettings.core;

import static org.junit.Assert.assertEquals;

import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.usersettings.UserSettingsProviderDescriptor;
import org.nuxeo.ecm.usersettings.UserSettingsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 */

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.usersettings.core", "org.nuxeo.ecm.usersettings.api",
        "org.nuxeo.ecm.usersettings.core.test",
        "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.userworkspace.core" })
public class TestUserSettings {

    @Inject
    CoreSession session;

    @Inject
    UserSettingsService service;

    @Inject
    RuntimeFeature runtime;

    @Test
    public void testRegisterProvider() throws Exception {
        assertEquals(5, service.getAllRegisteredProviders().size());
    }

    @Test
    public void testCategoriesMap() throws Exception {
        UserSettingsService service = Framework.getLocalService(UserSettingsService.class);
        Set<String> categories = service.getCategories();
        assertEquals(3, categories.size());
    }

    @Test
    public void testEnabledAttribute() throws Exception {
        UserSettingsService service = Framework.getLocalService(UserSettingsService.class);
        
        Set<String> categories = service.getCategories();
        assertEquals(3, categories.size());
    }

    @Test
    public void getEmptyList() throws ClientException {
        service.clearProviders();
        DocumentModelList docs = service.getCurrentSettingsByCategory(session, "default");
        assertEquals(0, docs.size());
    }

    @Test
    public void duplicateRegister() throws ClientException {
        ensureRegistered();
        Entry<String, UserSettingsProviderDescriptor> item = service.getAllRegisteredProviders().entrySet().iterator().next();
        service.registerProvider(item.getValue());
    }

    private void ensureRegistered() throws ClientException {
        if (!service.getAllRegisteredProviders().containsKey("TestSettings1")) {
            service.registerProvider(new UserSettingsProviderDescriptor(
                    "TestSettings1"));
        }
        if (!service.getAllRegisteredProviders().containsKey("TestSettings2")) {
            service.registerProvider(new UserSettingsProviderDescriptor(
                    "TestSettings2"));
        }
        assertEquals(2, service.getAllRegisteredProviders().size());
    }

    @Test
    public void nonExistentUnregister() throws ClientException {
        service.unRegisterProvider("dummy");
    }


}
