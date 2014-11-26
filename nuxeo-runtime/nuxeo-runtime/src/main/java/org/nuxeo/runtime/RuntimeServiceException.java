/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.runtime;

/**
 * A generic exception thrown by the Nuxeo Runtime.
 */
public class RuntimeServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RuntimeServiceException() {
    }

    public RuntimeServiceException(String message) {
        super(message);
    }

    public RuntimeServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeServiceException(Throwable cause) {
        super(cause);
    }

}
