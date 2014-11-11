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
 * Class for Security Exceptions
 *
 * @author tiry
 *
 */
public class DocumentSecurityException extends ClientException {

    private static final long serialVersionUID = 1768798758788L;

    public DocumentSecurityException(String message) {
        super(message);
    }

    public DocumentSecurityException(String message, Throwable cause) {
        super(message, WrappedException.wrap(cause));
    }


}
