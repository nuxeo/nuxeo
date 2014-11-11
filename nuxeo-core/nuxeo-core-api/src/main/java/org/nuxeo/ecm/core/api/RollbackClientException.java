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
 * An application that should rollback the transaction if any.
 * <p>
 * In an ejb container, the nuxeo-core-api module is package with the
 * nuxeo-core-facade module. The facade has a ejb-jar.xml that declare this
 * exception has rollback=true.
 *
 * @author arussel
 */
public class RollbackClientException extends ClientException {

    private static final long serialVersionUID = 1L;

    public RollbackClientException() {
    }

    public RollbackClientException(ClientException cause) {
        super(cause);
    }

    public RollbackClientException(String message, ClientException cause) {
        super(message, cause);
    }

    public RollbackClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public RollbackClientException(String message) {
        super(message);
    }

    public RollbackClientException(Throwable cause) {
        super(cause);
    }

}
