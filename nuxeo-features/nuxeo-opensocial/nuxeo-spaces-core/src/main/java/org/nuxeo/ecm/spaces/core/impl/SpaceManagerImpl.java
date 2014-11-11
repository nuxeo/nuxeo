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
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.SpaceProvider;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
import org.nuxeo.ecm.spaces.core.impl.contribs.SpaceContribDescriptor;
import org.nuxeo.ecm.spaces.core.impl.contribs.UniversContribDescriptor;
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

    private List<UniversContribDescriptor> universProvider = new ArrayList<UniversContribDescriptor>();

    private List<SpaceContribDescriptor> spaceProvider = new ArrayList<SpaceContribDescriptor>();

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

    private void removeUniversDescriptor(UniversContribDescriptor descriptor) {
        for (UniversContribDescriptor contrib : universProvider) {
            if (descriptor.getName().equals(contrib.getName())) {
                universProvider.remove(contrib);
                break;
            }
        }
    }

    private synchronized void manageUniversDescriptor(
            UniversContribDescriptor descriptor) {
        universProvider.add(descriptor);
    }

    private synchronized void manageSpaceDescriptor(
            SpaceContribDescriptor descriptor) {

        spaceProvider.add(descriptor);
        Collections.sort(spaceProvider);

    }

    /**
     * Universe list
     */
    public List<Univers> getUniversList(CoreSession session)
            throws SpaceException {
        List<Univers> list = new ArrayList<Univers>();

        for (UniversContribDescriptor descriptor : universProvider) {
            try {
                list.addAll(descriptor.getProvider().getAll(session));
            } catch (Exception e) {
                LOGGER.debug(e.getMessage(), e);
            }

        }
        return list;
    }

    /**
     * Get a univers
     *
     * @throws UniversNotFoundException , SpaceException
     */
    public Univers getUnivers(String name, CoreSession coreSession)
            throws UniversNotFoundException {

        for (UniversContribDescriptor descriptor : universProvider) {

            UniversProvider provider;
            try {
                provider = descriptor.getProvider();
                return provider.getUnivers(name, coreSession);
            } catch (UniversNotFoundException e) {
                // Do nothing
            } catch (Exception e) {
                LOGGER.error("Unable to get the provider for : "
                        + descriptor.getName());
            }
        }
        throw new UniversNotFoundException("No Univers with name : '" + name
                + "' was found");

    }

    /**
     * Space list for a given univers
     */
    public List<Space> getSpacesForUnivers(Univers univers,
            CoreSession coreSession) throws SpaceException {
        List<Space> list = new ArrayList<Space>();

        for (SpaceContribDescriptor descriptor : spaceProvider) {
            if (descriptor.matches(univers.getName())) {
                try {
                    list.addAll(descriptor.getProvider().getAll(coreSession));
                } catch (SpaceNotFoundException e) {
                    // This can be absolutely normal
                    continue;
                } catch (InstantiationException e) {
                    LOGGER.warn("Unable instanciate provider : "
                            + descriptor.getName(), e);
                } catch (IllegalAccessException e) {
                    LOGGER.warn("Unable to instanciate provider : "
                            + descriptor.getName(), e);
                }
            }
        }
        return list;
    }

    public Space getSpaceFromId(String spaceId, CoreSession session)
            throws SpaceException {
        DocumentRef spaceRef = new IdRef(spaceId);
        try {
            if (session.exists(spaceRef)) {
                return session.getDocument(spaceRef).getAdapter(Space.class);
            } else {
                throw new SpaceNotFoundException();
            }
        } catch (ClientException e) {
            throw new SpaceNotFoundException();
        }
    }

    public Space getSpace(String name, CoreSession session)
            throws SpaceException {
        return getSpace(name, (SpaceProvider) null, session);
    }

    @Deprecated
    public Space getSpace(String name, SpaceProvider provider,
            CoreSession session) throws SpaceException {
        for (SpaceContribDescriptor desc : spaceProvider) {
            try {
                Space space = desc.getProvider().getSpace(name, session);
                if (space != null) {
                    return space;
                }
            } catch (Exception e) {
                LOGGER.error("Unable to query provider " + desc.getName(), e);
            }
        }
        throw new SpaceNotFoundException();
    }

    public List<SpaceProvider> getSpacesProvider(Univers univers) {
        List<SpaceProvider> result = new ArrayList<SpaceProvider>();
        for (SpaceContribDescriptor desc : spaceProvider) {
            try {
                if(desc.matches(univers.getName())) {
                    result.add(desc.getProvider());
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to instanciate " + desc.getName(), e);
            }
        }
        return result;
    }

    public List<SpaceProvider> getSpacesProviders() {
        List<SpaceProvider> result = new ArrayList<SpaceProvider>();
        for (SpaceContribDescriptor desc : spaceProvider) {
            try {
                result.add(desc.getProvider());
            } catch (Exception e) {
                LOGGER.warn("Unable to instanciate " + desc.getName(), e);
            }
        }
        return result;
    }

    public String getProviderName(SpaceProvider provider) {
        for (SpaceContribDescriptor desc : spaceProvider) {
            try {
                if (desc.getProvider().equals(provider)) {
                    return desc.getName();
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to instanciate " + desc.getName(), e);
            }
        }
        return null;
    }

    public Univers getUniversFromId(String universId, CoreSession session)
            throws SpaceException {
        DocumentRef spaceRef = new IdRef(universId);
        try {
            if (session.exists(spaceRef)) {
                return session.getDocument(spaceRef).getAdapter(Univers.class);
            } else {
                throw new SpaceNotFoundException();
            }
        } catch (ClientException e) {
            throw new SpaceNotFoundException();
        }
    }

    public Space getSpace(String name, Univers univers, CoreSession session)
            throws SpaceException {

        for (SpaceProvider provider : getSpacesProvider(univers)) {
            try {
                return provider.getSpace(name, session);
            } catch (SpaceNotFoundException e) {
                continue;
            }
        }
        throw new SpaceNotFoundException();
    }

}
