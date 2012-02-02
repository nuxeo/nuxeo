/*
 * Copyright (c) 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 * A storage exception thrown if the connection was reset (and should be working
 * again). Helpful for callers that may want to retry things.
 *
 * @since 5.6
 */
public class ConnectionResetException extends StorageException {

    private static final long serialVersionUID = 1L;

    public ConnectionResetException() {
    }

    public ConnectionResetException(String message) {
        super(message);
    }

    public ConnectionResetException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionResetException(Throwable cause) {
        super(cause);
    }

}
