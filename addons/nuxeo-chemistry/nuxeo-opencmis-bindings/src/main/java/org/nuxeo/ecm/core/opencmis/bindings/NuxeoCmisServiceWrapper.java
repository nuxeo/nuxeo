/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.ConformanceCmisServiceWrapper;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CMIS Conformance Service Wrapper that has better exception handling than the default.
 */
public class NuxeoCmisServiceWrapper extends ConformanceCmisServiceWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(NuxeoCmisServiceWrapper.class);

    public NuxeoCmisServiceWrapper(CmisService service) {
        super(service);
    }

    /**
     * Converts the given exception into a CMIS exception.
     */
    @Override
    protected CmisBaseException createCmisException(Exception e) {
        // make sure that the transaction is marked rollback-only, as higher layers in the
        // CMIS services stack will swallow it and turn it into a high-level HTTP error
        TransactionHelper.setTransactionRollbackOnly();

        // map exception into CmisBaseException
        if (e == null) {
            return new CmisRuntimeException("Unknown exception!");
        } else if (e instanceof CmisBaseException) {
            return (CmisBaseException) e;
        } else if (e instanceof RecoverableClientException) {
            return new CmisRuntimeException("error", e);
        } else {
            // should not happen if the connector works correctly
            // it's alarming enough to log the exception
            LOG.warn(e.toString(), e);
            return new CmisRuntimeException(e.getMessage(), e);
        }
    }

}
