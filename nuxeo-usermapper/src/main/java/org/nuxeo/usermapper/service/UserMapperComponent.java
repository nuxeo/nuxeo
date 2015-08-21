/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.usermapper.service;

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
 *
 */
public class UserMapperComponent extends DefaultComponent implements
        UserMapperService {

    protected static final Log log = LogFactory.getLog(UserMapperComponent.class);

    protected Map<String, UserMapper> mappers = new HashMap<String, UserMapper>();

    protected List<UserMapperDescriptor> descriptors = new ArrayList<>();

    public static final String MAPPER_EP = "mapper";

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (MAPPER_EP.equalsIgnoreCase(extensionPoint)) {
            UserMapperDescriptor desc = (UserMapperDescriptor) contribution;
            descriptors.add(desc);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        for (UserMapperDescriptor desc : descriptors) {
            try {
                mappers.put(desc.name, desc.getInstance());
            } catch (Exception e) {
                log.error("Unable to register mapper " + desc.name, e);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            {
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
    public void deactivate(ComponentContext context) {
        for (UserMapper um : mappers.values()) {
            um.release();
        }
        super.deactivate(context);
    }

    public UserMapper getMapper(String mappingName) throws NuxeoException {
        UserMapper um = mappers.get(mappingName);
        if (um == null) {
            throw new NuxeoException("No mapping defined for " + mappingName);
        }
        return um;
    }

    @Override
    public NuxeoPrincipal getCreateOrUpdateNuxeoPrincipal(String mappingName,
            Object userObject) throws NuxeoException {
        return getMapper(mappingName).getCreateOrUpdateNuxeoPrincipal(
                userObject);
    }

    @Override
    public Object wrapNuxeoPrincipal(String mappingName,
            NuxeoPrincipal principal) throws NuxeoException {
        return getMapper(mappingName).wrapNuxeoPrincipal(principal);
    }

    @Override
    public Set<String> getAvailableMappings() {
        return mappers.keySet();
    }

}
