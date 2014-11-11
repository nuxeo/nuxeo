/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     arussel
 */
package org.nuxeo.ecm.core.api;

/**
 * @author arussel
 *
 */
public class ClientRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClientRuntimeException() {
    }

    public ClientRuntimeException(String message) {
        super(message);
    }

    public ClientRuntimeException(Throwable t) {
        super(t);
    }

    public ClientRuntimeException(String message, Throwable t) {
        super(message, t);
    }
}
