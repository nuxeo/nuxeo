/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.usermapper.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.usermapper.extension.UserMapper;

/**
 * Component to manage extension point and expose the {@link UserMapperService} interface.
 *
 * @author tiry
 * @since 7.4
 */
public class UserMapperComponent extends DefaultComponent implements UserMapperService {

    protected static final Log log = LogFactory.getLog(UserMapperComponent.class);

    protected Map<String, UserMapper> mappers = new HashMap<>();

    protected List<UserMapperDescriptor> descriptors = new ArrayList<>();

    public static final String MAPPER_EP = "mapper";

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (MAPPER_EP.equalsIgnoreCase(extensionPoint)) {
            UserMapperDescriptor desc = (UserMapperDescriptor) contribution;
            descriptors.add(desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (MAPPER_EP.equalsIgnoreCase(extensionPoint)) {
            UserMapperDescriptor desc = (UserMapperDescriptor) contribution;
            UserMapper um = mappers.get(desc.name);
            if (um != null) {
                um.release();
                mappers.remove(desc.name);
            }
        }
    }

    @Override
    public void start(ComponentContext context) {
        for (UserMapperDescriptor desc : descriptors) {
            try {
                mappers.put(desc.name, desc.getInstance());
            } catch (Exception e) {
                log.error("Unable to register mapper " + desc.name, e);
            }
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        for (UserMapper um : mappers.values()) {
            um.release();
        }
        super.deactivate(context);
    }

    @Override
    public UserMapper getMapper(String mappingName) throws NuxeoException {
        UserMapper um = mappers.get(mappingName);
        if (um == null) {
            throw new NuxeoException("No mapping defined for " + mappingName);
        }
        return um;
    }

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(String mappingName, Object userObject)
            throws NuxeoException {
        return getOrCreateAndUpdateNuxeoPrincipal(mappingName, userObject, true, true, null);
    }

    @Override
    public NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(String mappingName, Object userObject,
            boolean createIfNeeded, boolean update, Map<String, Serializable> params) throws NuxeoException {
        return getMapper(mappingName).getOrCreateAndUpdateNuxeoPrincipal(userObject, createIfNeeded, update, params);
    }

    @Override
    public Object wrapNuxeoPrincipal(String mappingName, NuxeoPrincipal principal, Object nativePrincipal,
            Map<String, Serializable> params) throws NuxeoException {
        return getMapper(mappingName).wrapNuxeoPrincipal(principal, nativePrincipal, params);
    }

    @Override
    public Set<String> getAvailableMappings() {
        return mappers.keySet();
    }

}
