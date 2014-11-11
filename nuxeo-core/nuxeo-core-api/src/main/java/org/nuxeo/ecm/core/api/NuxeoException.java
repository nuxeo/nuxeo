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
package org.nuxeo.ecm.core.api;

/**
 * The most generic exception thrown by the Nuxeo Core.
 */
public class NuxeoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NuxeoException() {
    }

    public NuxeoException(String message) {
        super(message);
    }

    public NuxeoException(String message, Throwable cause) {
        super(message, cause);
    }

    public NuxeoException(Throwable cause) {
        super(cause);
    }

}
