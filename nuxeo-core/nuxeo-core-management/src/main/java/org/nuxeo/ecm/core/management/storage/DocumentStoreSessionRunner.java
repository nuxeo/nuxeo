/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.core.management.storage;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.management.api.StorageError;
import org.nuxeo.runtime.api.Framework;

/**
 * Runner dedicated to mgmt doc operations
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
public abstract class DocumentStoreSessionRunner extends
        UnrestrictedSessionRunner {

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
            throw new IllegalArgumentException("Session is not attached to "
                    + repositoryName);
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
        } catch (ClientException e) {
            throw new StorageError("Storage error :  " + errorMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(jarCL);
        }
    }

    protected String errorMessage() {
        return String.format("%s:%s", getClass().getCanonicalName(),
                this.toString());
    }

}
