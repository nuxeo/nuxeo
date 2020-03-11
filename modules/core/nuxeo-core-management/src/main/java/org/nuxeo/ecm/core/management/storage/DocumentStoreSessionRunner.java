/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */
package org.nuxeo.ecm.core.management.storage;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.management.api.StorageError;
import org.nuxeo.runtime.api.Framework;

/**
 * Runner dedicated to mgmt doc operations
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
public abstract class DocumentStoreSessionRunner extends UnrestrictedSessionRunner {

    protected static String repositoryName;

    /**
     * Should be Invoked at startup
     */
    public static void setRepositoryName(String name) {
        repositoryName = name;
    }

    public DocumentStoreSessionRunner() {
        super(repositoryName);
    }

    public DocumentStoreSessionRunner(CoreSession session) {
        super(session);
        if (!repositoryName.equals(session.getRepositoryName())) {
            throw new IllegalArgumentException("Session is not attached to " + repositoryName);
        }
    }

    /**
     * Run with the nuxeo class loader, wrap client exception into errors
     */
    public void runSafe() {
        ClassLoader jarCL = Thread.currentThread().getContextClassLoader();
        ClassLoader bundleCL = Framework.class.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(bundleCL);
            runUnrestricted();
        } catch (NuxeoException e) {
            throw new StorageError("Storage error :  " + errorMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(jarCL);
        }
    }

    protected String errorMessage() {
        return String.format("%s:%s", getClass().getCanonicalName(), this.toString());
    }

}
