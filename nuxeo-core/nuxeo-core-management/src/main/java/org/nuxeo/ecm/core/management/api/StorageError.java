/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
package org.nuxeo.ecm.core.management.api;

/**
 * Raised if something goes wrong with the storage
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
public class StorageError extends Error {

    public StorageError() {
        super();
    }

    public StorageError(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageError(String message) {
        super(message);
        throw new UnsupportedOperationException();
    }

    public StorageError(Throwable cause) {
        super(cause);
        throw new UnsupportedOperationException();
    }

    private static final long serialVersionUID = 1L;

}
