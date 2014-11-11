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
package org.nuxeo.ecm.core.api;

/**
 * An exception thrown if a concurrent update was detected.
 * <p>
 * Helpful for callers that may want to retry things.
 *
 * @since 5.8
 */
public class ConcurrentUpdateException extends ClientException {

    private static final long serialVersionUID = 1L;

    public ConcurrentUpdateException() {
    }

    public ConcurrentUpdateException(String message) {
        super(message);
    }

    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConcurrentUpdateException(Throwable cause) {
        super(cause);
    }

}
