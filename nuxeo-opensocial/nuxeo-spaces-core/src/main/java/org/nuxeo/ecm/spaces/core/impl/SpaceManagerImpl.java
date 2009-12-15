/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.SpaceProvider;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceSecurityException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
import org.nuxeo.ecm.spaces.core.impl.exceptions.NoElementFoundException;
import org.nuxeo.ecm.spaces.core.impl.exceptions.OperationNotSupportedException;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 * @author 10044893
 *
 */
public class SpaceManagerImpl extends DefaultComponent implements SpaceManager {

    private static final Log LOGGER = LogFactory.getLog(SpaceManagerImpl.class);

    private static final String UNIVERS_CONTRIB = "universContrib";
    private static final String SPACE_CONTRIB = "spaceContrib";

    private List<UniversContribDescriptor> universProvider;
    private List<SpaceContribDescriptor> spaceProvider;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (UNIVERS_CONTRIB.equals(extensionPoint)) {
            UniversContribDescriptor descriptor = (UniversContribDescriptor) contribution;

            if (descriptor.isRemove()) {
                removeUniversDescriptor(descriptor);
            } else {
                manageUniversDescriptor(descriptor);
            }
        } else if (SPACE_CONTRIB.equals(extensionPoint)) {
            SpaceContribDescriptor descriptor = (SpaceContribDescriptor) contribution;
            manageSpaceDescriptor(descriptor);
        }

    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        // TODO

    }

    private synchronized void manageUniversDescriptor(
            UniversContribDescriptor descriptor) {
        if (provider != null) {
            registeredUniversProviders.add(getOrderOrMax(descriptor.getOrder(),
                    registeredUniversProviders.size()),
                    new DescriptorUniversProviderPair(provider, descriptor));
        }
    }

    private synchronized void manageSpaceDescriptor(
            SpaceContribDescriptor descriptor) {
        registeredSpacesProviders.add(getOrderOrMax(descriptor.getOrder(),
                registeredSpacesProviders.size()),
                new DescriptorSpaceProviderPair(descriptor, provider));

    }

    private int getOrderOrMax(String value, int max) {
        int order = 0;
        try {
            if (value != null)
                order = Integer.parseInt(value);
        } catch (Exception e) {
            LOGGER.error(e);
        }
        if (order <= max) {
            return order;
        } else {
            return max;
        }
    }

    /**
     * Universe list
     */
    public List<Univers> getUniversList() throws SpaceException {
        List<Univers> list = new ArrayList<Univers>();

        for (UniversContribDescriptor descriptor : universProvider) {
            try {
                list.addAll(descriptor.getProvider().values());
            } catch (Exception e) {
                LOGGER.debug(e.getMessage(), e);
            }

        }
        return list;
    }

    /**
     * Get a univers
     *
     * @throws UniversNotFoundException
     *             , SpaceException
     */
    public Univers getUnivers(String name, CoreSession coreSession)
            throws SpaceException {

        for (UniversContribDescriptor descriptor : universProvider) {

            UniversProvider provider;
            try {
                provider = descriptor.getProvider();

                if (provider.containsKey(name)) {
                    return provider.get(name);
                }
            } catch (Exception e) {
                LOGGER.error("Unable to get the provider for : "
                        + descriptor.getName());
            }
        }
        throw new SpaceException("No Univers with name : '" + name
                + "' was found");

    }

    /**
     * Space list for a given univers
     */
    public List<Space> getSpacesForUnivers(Univers univers,
            CoreSession coreSession) throws SpaceException {
        List<Space> list = new ArrayList<Space>();

        for (SpaceContribDescriptor descriptor : spaceProvider) {

            String pattern = descriptor.getPattern();
            if (pattern == null || pattern.equals("*"))
                pattern = ".*";
            if (Pattern.matches(pattern, univers.getName())) {
                try {
                    list.addAll(descriptor.getProvider());
                } catch (Exception e) {
                        LOGGER.error("Unable to get the provider for : "
                                + descriptor.getName());
                }

            }
        }
        return list;
    }

    public void deleteSpace(Space space) throws SpaceException {
        // TODO Auto-generated method stub

    }

    public void deleteUnivers(Univers univers) throws SpaceException {
        // TODO Auto-generated method stub

    }

    public Space getSpace(String spaceId) throws SpaceException {
        // TODO Auto-generated method stub
        return null;
    }

    public Space getSpace(String name, SpaceProvider provider)
            throws SpaceException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Space> getSpacesForUnivers(Univers universe)
            throws UniversNotFoundException, SpaceException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<SpaceProvider> getSpacesProvider(Univers univers) {
        // TODO Auto-generated method stub
        return null;
    }

    public Univers getUnivers(String name) throws UniversNotFoundException,
            SpaceException {
        // TODO Auto-generated method stub
        return null;
    }

    public Univers getUniversFromId(String universId) throws SpaceException {
        // TODO Auto-generated method stub
        return null;
    }

    public Space updateSpace(Space newSpace) throws SpaceException {
        // TODO Auto-generated method stub
        return null;
    }




}