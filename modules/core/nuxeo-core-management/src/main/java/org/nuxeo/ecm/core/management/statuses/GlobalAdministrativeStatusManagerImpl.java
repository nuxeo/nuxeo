/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.management.statuses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.core.management.api.GlobalAdministrativeStatusManager;
import org.nuxeo.ecm.core.management.storage.AdministrativeStatusPersister;
import org.nuxeo.ecm.core.management.storage.DocumentModelStatusPersister;

public class GlobalAdministrativeStatusManagerImpl implements GlobalAdministrativeStatusManager {

    protected final AdministrativeStatusPersister persister = new DocumentModelStatusPersister();

    protected final Map<String, AdministrativeStatusManager> managers = new HashMap<>();

    protected final List<AdministrableServiceDescriptor> descriptors = new ArrayList<>();

    protected final Map<String, AdministrableServiceDescriptor> descriptorsByServiceId = new HashMap<>();

    @Override
    public String getLocalNuxeoInstanceIdentifier() {
        return NuxeoInstanceIdentifierHelper.getServerInstanceName();
    }

    @Override
    public AdministrativeStatusManager getStatusManager(String instanceIdentifier) {
        if (!managers.containsKey(instanceIdentifier)) {
            managers.put(instanceIdentifier, new AdministrativeStatusManagerImpl(this, persister, instanceIdentifier));
        }
        return managers.get(instanceIdentifier);
    }

    @Override
    public List<String> listInstanceIds() {
        return persister.getAllInstanceIds();
    }

    @Override
    public void setStatus(String serviceIdentifier, String state, String message, String login) {
        for (String instanceIdentifier : listInstanceIds()) {
            getStatusManager(instanceIdentifier).setStatus(serviceIdentifier, state, message, login);
        }
    }

    @Override
    public void registerService(AdministrableServiceDescriptor desc) {
        descriptors.add(desc);
        descriptorsByServiceId.put(desc.getId(), desc);
    }

    @Override
    public List<AdministrableServiceDescriptor> listRegistredServices() {
        return descriptors;
    }

    @Override
    public AdministrableServiceDescriptor getServiceDescriptor(String serviceIdentifier) {
        return descriptorsByServiceId.get(serviceIdentifier);
    }

}
