/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import java.util.function.Supplier;

import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUpdateConflictException;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.support.wrapper.ConformanceCmisServiceWrapper;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.query.QueryParseException;
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
            return new CmisRuntimeException(e.getMessage(), e);
        } else if (e instanceof QueryParseException) {
            return new CmisInvalidArgumentException(e.getMessage(), e);
        } else if (e instanceof ConcurrentUpdateException) {
            return new CmisUpdateConflictException(e.getMessage(), e);
        } else {
            // should not happen if the connector works correctly
            // it's alarming enough to log the exception
            LOG.warn(e.toString(), e);
            return new CmisRuntimeException(e.getMessage(), e);
        }
    }

    protected void runWithRetryOnConflict(Runnable runnable) {
        runWithRetryOnConflict(() -> {
            runnable.run();
            return null;
        });
    }

    protected <R> R runWithRetryOnConflict(Supplier<R> supplier) {
        try {
            return supplier.get();
        } catch (CmisUpdateConflictException e) {
            // retry once
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            return supplier.get();
        }
    }

    @Override
    public void deleteObject(String repositoryId, String objectId, Boolean allVersions, ExtensionsData extension) {
        runWithRetryOnConflict(() -> super.deleteObject(repositoryId, objectId, allVersions, extension));
    }

    @Override
    public void deleteObjectOrCancelCheckOut(String repositoryId, String objectId, Boolean allVersions,
            ExtensionsData extension) {
        runWithRetryOnConflict(
                () -> super.deleteObjectOrCancelCheckOut(repositoryId, objectId, allVersions, extension));
    }

    @Override
    public FailedToDeleteData deleteTree(String repositoryId, String folderId, Boolean allVersions,
            UnfileObject unfileObjects, Boolean continueOnFailure, ExtensionsData extension) {
        return runWithRetryOnConflict(() -> super.deleteTree(repositoryId, folderId, allVersions, unfileObjects,
                continueOnFailure, extension));
    }

}
