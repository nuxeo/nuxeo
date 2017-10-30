/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.automation.core.operations.services;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 9.3
 */
@Operation(id = AuditRestore.ID, category = Constants.CAT_SERVICES, label = "Restore log entries", description = "Restore log entries from an audit storage implementation to the audit backend.")
public class AuditRestore {

    public static final String ID = "Audit.Restore";

    public static final int DEFAULT_BATCH_SIZE = 100;

    public static final int DEFAULT_KEEP_ALIVE = 10;

    @Param(name = "auditStorageClass")
    protected String auditStorageClass;

    @Param(name = "batchSize", required = false)
    protected int batchSize = DEFAULT_BATCH_SIZE;

    @Param(name = "keepAlive", required = false)
    protected int keepAlive = DEFAULT_KEEP_ALIVE;

    @OperationMethod
    public void run() throws OperationException {
        try {
            Class clazz = Class.forName(auditStorageClass);
            if (!AuditStorage.class.isAssignableFrom(clazz)) {
                throw new OperationException(
                        "The class " + auditStorageClass + " does not implement the AuditStorage interface");
            }
            AuditStorage auditStorage = (AuditStorage) clazz.getDeclaredConstructor().newInstance();
            Logs auditBackend = (Logs) Framework.getService(AuditLogger.class);
            auditBackend.restore(auditStorage, batchSize, keepAlive);
        } catch (ReflectiveOperationException e) {
            throw new OperationException("Cannot create audit storage of type " + auditStorageClass, e);
        }
    }

}
