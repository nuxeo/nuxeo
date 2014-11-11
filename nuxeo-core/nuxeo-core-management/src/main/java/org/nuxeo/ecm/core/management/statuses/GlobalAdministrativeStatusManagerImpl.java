/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

public class GlobalAdministrativeStatusManagerImpl implements
        GlobalAdministrativeStatusManager {

    protected AdministrativeStatusPersister persister = new DocumentModelStatusPersister();

    protected Map<String, AdministrativeStatusManager> managers = new HashMap<String, AdministrativeStatusManager>();

    protected List<AdministrableServiceDescriptor> descriptors = new ArrayList<AdministrableServiceDescriptor>();

    protected Map<String, AdministrableServiceDescriptor> descriptorsByServiceId = new HashMap<String, AdministrableServiceDescriptor>();

    @Override
    public String getLocalNuxeoInstanceIdentifier() {
        return NuxeoInstanceIdentifierHelper.getServerInstanceName();
    }

    @Override
    public AdministrativeStatusManager getStatusManager(
            String instanceIdentifier) {
        if (!managers.containsKey(instanceIdentifier)) {
            managers.put(instanceIdentifier,
                    new AdministrativeStatusManagerImpl(this, persister,
                            instanceIdentifier));
        }
        return managers.get(instanceIdentifier);
    }

    @Override
    public List<String> listInstanceIds() {
        return persister.getAllInstanceIds();
    }

    @Override
    public void setStatus(String serviceIdentifier, String state,
            String message, String login) {
        for (String instanceIdentifier : listInstanceIds()) {
            getStatusManager(instanceIdentifier).setStatus(serviceIdentifier,
                    state, message, login);
        }
    }

    public void registerService(AdministrableServiceDescriptor desc) {
        descriptors.add(desc);
        descriptorsByServiceId.put(desc.getId(), desc);
    }

    @Override
    public List<AdministrableServiceDescriptor> listRegistredServices() {
        return descriptors;
    }

    public AdministrableServiceDescriptor getServiceDescriptor(
            String serviceIndentifier) {
        return descriptorsByServiceId.get(serviceIndentifier);
    }

}
