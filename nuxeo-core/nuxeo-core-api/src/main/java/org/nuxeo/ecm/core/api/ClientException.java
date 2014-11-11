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

package org.nuxeo.ecm.core.api;

/**
 * The most generic exception thrown by the Nuxeo Core.
 *
 * @author bstefanescu
 */
public class ClientException extends Exception {

    private static final long serialVersionUID = 829907884555472415L;

    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, ClientException cause) {
        super(message, cause);
    }

    public ClientException(String message, Throwable cause) {
        super(message, WrappedException.wrap(cause));
    }

    public ClientException(Throwable cause) {
        super(WrappedException.wrap(cause));
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
