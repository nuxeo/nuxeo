/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

/**
 * A storage exception thrown if a concurrent update was detected.
 * <p>
 * Helpful for callers that may want to retry things.
 *
 * @since 5.8
 */
public class ConcurrentUpdateStorageException extends StorageException {

    private static final long serialVersionUID = 1L;

    public ConcurrentUpdateStorageException() {
    }

    public ConcurrentUpdateStorageException(String message) {
        super(message);
    }

    public ConcurrentUpdateStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConcurrentUpdateStorageException(Throwable cause) {
        super(cause);
    }

}
