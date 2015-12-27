/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core.api;

/**
 * Exception thrown when access to a document is denied.
 *
 * @since 5.6
 */
public class DocumentSecurityException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public DocumentSecurityException() {
        super();
    }

    public DocumentSecurityException(String message) {
        super(message);
    }

    public DocumentSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentSecurityException(Throwable cause) {
        super(cause);
    }

}
