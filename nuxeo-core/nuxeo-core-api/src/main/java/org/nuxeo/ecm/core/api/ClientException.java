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
 * Exception thrown by the Nuxeo Core.
 */
public class ClientException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, ClientException cause) {
        super(message, cause);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }

    public ClientException(ClientException cause) {
        super(cause);
    }

    public static ClientException wrap(Throwable exception) {
        ClientException clientException;

        if (null == exception) {
            clientException = new ClientException(
                    "Root exception was null. Please check your code.");
        } else {
            if (exception instanceof ClientException) {
                clientException = (ClientException) exception;
            } else {
                clientException = new ClientException(
                        exception.getLocalizedMessage(), exception);
            }
        }
        return clientException;
    }

}
