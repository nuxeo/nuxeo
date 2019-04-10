/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.client;

import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.BindingsObjectFactoryImpl;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.spi.AuthenticationProvider;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;

/**
 * Local CMIS binding to the services.
 */
public class NuxeoBinding implements CmisBinding {

    private static final BindingsObjectFactory OBJECT_FACTORY = new BindingsObjectFactoryImpl();

    public final CmisService service;

    private NuxeoCmisService nuxeoCmisService;

    public NuxeoBinding(CmisService service) {
        this.service = service;
    }

    @Override
    public void close() {
        service.close();
    }

    @Override
    public CmisService getRepositoryService() {
        return service;
    }

    @Override
    public CmisService getNavigationService() {
        return service;
    }

    @Override
    public CmisService getObjectService() {
        return service;
    }

    @Override
    public CmisService getDiscoveryService() {
        return service;
    }

    @Override
    public CmisService getRelationshipService() {
        return service;
    }

    @Override
    public CmisService getVersioningService() {
        return service;
    }

    @Override
    public CmisService getAclService() {
        return service;
    }

    @Override
    public CmisService getMultiFilingService() {
        return service;
    }

    @Override
    public CmisService getPolicyService() {
        return service;
    }

    @Override
    public BindingsObjectFactory getObjectFactory() {
        return OBJECT_FACTORY;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return null; // no provider
    }

    @Override
    public void clearAllCaches() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearRepositoryCache(String repositoryId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSessionId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public BindingType getBindingType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public CoreSession getCoreSession() {
        return getNuxeoCmisService() == null ? null : nuxeoCmisService.getCoreSession();
    }

    /**
     * Gets the potentially wrapped NuxeoCmisService.
     */
    public NuxeoCmisService getNuxeoCmisService() {
        if (nuxeoCmisService == null) {
            nuxeoCmisService = NuxeoCmisService.extractFromCmisService(service);
        }
        return nuxeoCmisService;
    }

}
