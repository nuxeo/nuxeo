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
package org.nuxeo.ecm.core.opencmis.bindings;

import java.math.BigInteger;

import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.impl.server.AbstractServiceFactory;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.CmisServiceWrapper;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepositories;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;
import org.nuxeo.runtime.api.Framework;

/**
 * Factory for a wrapped {@link NuxeoCmisService}.
 * <p>
 * Called for each method dispatch by
 * {@link org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet}
 * or
 * {@link org.apache.chemistry.opencmis.server.impl.webservices.AbstractService}.
 */
public class NuxeoCmisServiceFactory extends AbstractServiceFactory {

    public static final BigInteger DEFAULT_TYPES_MAX_ITEMS = BigInteger.valueOf(100);
    public static final BigInteger DEFAULT_TYPES_DEPTH = BigInteger.valueOf(-1);
    public static final BigInteger DEFAULT_MAX_ITEMS = BigInteger.valueOf(100);
    public static final BigInteger DEFAULT_DEPTH = BigInteger.valueOf(2);

    @Override
    public CmisService getService(CallContext context) {
        String repositoryId = context.getRepositoryId();
        if (StringUtils.isBlank(repositoryId)) {
            repositoryId = null;
        } else {
            NuxeoRepository repository = Framework.getService(
                    NuxeoRepositories.class).getRepository(repositoryId);
            if (repository == null) {
                throw new CmisInvalidArgumentException("No such repository: "
                        + repositoryId);
            }
        }
        NuxeoCmisService service = new NuxeoCmisService(repositoryId, context);

        // wrap the service to provide default parameter checks
        return new CmisServiceWrapper<NuxeoCmisService>(service,
                DEFAULT_TYPES_MAX_ITEMS, DEFAULT_TYPES_DEPTH,
                DEFAULT_MAX_ITEMS, DEFAULT_DEPTH);
    }

}
