/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl.server;

import java.math.BigInteger;
import java.util.Map;

import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.CmisServiceWrapper;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Factory for a wrapped {@link NuxeoCmisService}.
 */
public class NuxeoServiceFactory extends AbstractServiceFactory {

    public static final String PARAM_NUXEO_SESSION_ID = "NUXEO_SESSION_ID";

    public static final BigInteger DEFAULT_TYPES_MAX_ITEMS = BigInteger.valueOf(100);

    public static final BigInteger DEFAULT_TYPES_DEPTH = BigInteger.valueOf(-1);

    public static final BigInteger DEFAULT_MAX_ITEMS = BigInteger.valueOf(100);

    public static final BigInteger DEFAULT_DEPTH = BigInteger.valueOf(2);

    private ThreadLocal<CmisServiceWrapper<NuxeoCmisService>> threadLocalService = new ThreadLocal<CmisServiceWrapper<NuxeoCmisService>>();

    private CoreSession coreSession;

    private NuxeoRepository nuxeoRepository;

    @Override
    public void init(Map<String, String> parameters) {
        String sid = parameters.get(PARAM_NUXEO_SESSION_ID);
        coreSession = CoreInstance.getInstance().getSession(sid);
        nuxeoRepository = new NuxeoRepository(coreSession.getRepositoryName());
    }

    @Override
    public void destroy() {
        threadLocalService = null;
    }

    @Override
    public CmisService getService(CallContext context) {
        CmisServiceWrapper<NuxeoCmisService> service = threadLocalService.get();
        if (service == null) {
            NuxeoCmisService s = new NuxeoCmisService(coreSession,
                    nuxeoRepository);
            // wrap the service to provide default parameter checks
            service = new CmisServiceWrapper<NuxeoCmisService>(s,
                    DEFAULT_TYPES_MAX_ITEMS, DEFAULT_TYPES_DEPTH,
                    DEFAULT_MAX_ITEMS, DEFAULT_DEPTH);
            threadLocalService.set(service);
        }
        service.getWrappedService().setCallContext(context);
        return service;
    }

}
